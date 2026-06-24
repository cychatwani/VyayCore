package com.vyay.core.services.expense;

import com.github.f4b6a3.uuid.UuidCreator;
import com.vyay.core.common.security.HmacSigner;
import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.dto.requests.expense.CreateExpenseRequestDTO;
import com.vyay.core.dto.requests.expense.ExpensePayerInputDTO;
import com.vyay.core.dto.requests.expense.ExpenseShareInputDTO;
import com.vyay.core.dto.response.expense.ExpenseResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.BalanceLedgerEntry;
import com.vyay.core.entity.expense.Expense;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.LedgerSourceType;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.enums.SplitType;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InvalidCurrencyException;
import com.vyay.core.exception.business.InvalidExpenseException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.repository.*;
import com.vyay.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CurrencyRepository currencyRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final ExpenseSplitCalculator splitCalculator;
    private final ExpenseLineFactory lineFactory;
    private final BalanceRepository balanceRepository;
    private final BalanceLedgerRepository ledgerRepository;
    private final HmacSigner hmacSigner;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CurrencyRepository currencyRepository,
                          GroupRepository groupRepository,
                          GroupMembershipRepository groupMembershipRepository,
                          UserRepository userRepository,
                          ExpenseSplitCalculator splitCalculator,
                          ExpenseLineFactory lineFactory,
                          BalanceRepository balanceRepository,
                          BalanceLedgerRepository ledgerRepository,
                          HmacSigner hmacSigner) {
        this.expenseRepository = expenseRepository;
        this.currencyRepository = currencyRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.splitCalculator = splitCalculator;
        this.lineFactory = lineFactory;
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.hmacSigner = hmacSigner;
    }

    @Transactional
    public ExpenseResponseDTO createExpense(User principal, CreateExpenseRequestDTO request) {
        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new InvalidCurrencyException(request.getCurrencyCode()));

        long total = MoneyUtils.toMinor(request.getTotalAmount(), currency);
        if (total <= 0) {
            throw new InvalidExpenseException("Total amount must be positive");
        }

        Group group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .filter(g -> !g.isDeleted())
                    .orElseThrow(GroupNotFoundException::new);
            requireActiveMember(group.getId(), principal.getId());
        }

        Expense expense = Expense.builder()
                .description(request.getDescription())
                .totalAmountMinor(total)
                .currency(currency)
                .group(group)
                .createdBy(userRepository.getReferenceById(principal.getId()))
                .splitType(group == null ? null : requireSplitType(request.getSplitType()))
                .expenseDate(request.getExpenseDate())
                .notes(request.getNotes())
                .build();

        if (group == null) {
            expense.getPayers().add(lineFactory.payer(expense, principal, total));
            expense.getShares().add(lineFactory.share(expense, principal, total, null, null));
        } else {
            buildPayers(expense, group, request.getPayers(), total, currency);
            buildShares(expense, group, request.getSplitType(), request.getParticipants(), total, currency);
        }

        Expense saved = expenseRepository.save(expense);

        if (saved.getGroup() != null) {
            applyBalanceDeltas(saved);
        }

        return ExpenseResponseDTO.from(saved);
    }

    private void applyBalanceDeltas(Expense expense) {
        UUID groupId = expense.getGroup().getId();
        String currencyCode = expense.getCurrency().getCode();

        Map<UUID, Long> netByUser = new HashMap<>();
        for (var p : expense.getPayers()) {
            netByUser.merge(p.getUser().getId(), p.getAmountPaidMinor(), Long::sum);
        }
        for (var s : expense.getShares()) {
            netByUser.merge(s.getUser().getId(), -s.getOwedAmountMinor(), Long::sum);
        }

        netByUser.forEach((userId, delta) -> {
            if (delta == 0) {
                return;
            }
            balanceRepository.applyDelta(UuidCreator.getTimeOrderedEpoch(), groupId, userId, expense.getCurrency().getId(), delta);

            BalanceLedgerEntry entry = hmacSigner.signedEntry(
                    expense.getGroup(),
                    userRepository.getReferenceById(userId),
                    expense.getCurrency(),
                    delta,
                    LedgerSourceType.EXPENSE,
                    expense.getId());
            ledgerRepository.save(entry);
        });
    }

    private void buildPayers(Expense expense, Group group, List<ExpensePayerInputDTO> inputs,
                             long total, Currency currency) {
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidExpenseException("A group expense needs at least one payer");
        }
        Set<UUID> seen = new HashSet<>();
        long sum = 0;
        for (ExpensePayerInputDTO in : inputs) {
            if (!seen.add(in.getUserId())) {
                throw new InvalidExpenseException("Duplicate payer: " + in.getUserId());
            }
            long paid = MoneyUtils.toMinor(in.getAmountPaid(), currency);
            if (paid <= 0) {
                throw new InvalidExpenseException("Each payer must have a positive amount");
            }
            expense.getPayers().add(lineFactory.payer(expense, resolveMember(group, in.getUserId()), paid));
            sum += paid;
        }
        if (sum != total) {
            throw new InvalidExpenseException("Payer amounts (" + sum + ") must sum to the total (" + total + ")");
        }
    }

    private void buildShares(Expense expense, Group group, SplitType type,
                             List<ExpenseShareInputDTO> inputs, long total, Currency currency) {
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidExpenseException("A split needs at least one participant");
        }
        Set<UUID> seen = new HashSet<>();
        List<User> users = new ArrayList<>(inputs.size());
        for (ExpenseShareInputDTO in : inputs) {
            if (!seen.add(in.getUserId())) {
                throw new InvalidExpenseException("Duplicate participant: " + in.getUserId());
            }
            users.add(resolveMember(group, in.getUserId()));
        }

        long[] owed = switch (type) {
            case EQUAL -> splitCalculator.equally(total, inputs.size());
            case EXACT -> splitCalculator.exact(total, exactMinors(inputs, currency));
            case PERCENTAGE -> splitCalculator.byPercentage(total,
                    inputs.stream().map(ExpenseShareInputDTO::getPercentage).toList());
            case SHARES -> splitCalculator.byWeight(total,
                    inputs.stream().map(ExpenseShareInputDTO::getShareWeight).toList());
        };

        for (int i = 0; i < inputs.size(); i++) {
            BigDecimal pct = (type == SplitType.PERCENTAGE) ? inputs.get(i).getPercentage() : null;
            Integer weight = (type == SplitType.SHARES) ? inputs.get(i).getShareWeight() : null;
            expense.getShares().add(lineFactory.share(expense, users.get(i), owed[i], pct, weight));
        }
    }

    private long[] exactMinors(List<ExpenseShareInputDTO> inputs, Currency currency) {
        long[] out = new long[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            BigDecimal amt = inputs.get(i).getExactAmount();
            if (amt == null) {
                throw new InvalidExpenseException("EXACT split needs an amount per participant");
            }
            out[i] = MoneyUtils.toMinor(amt, currency);
        }
        return out;
    }

    private SplitType requireSplitType(SplitType type) {
        if (type == null) {
            throw new InvalidExpenseException("splitType is required for a group expense");
        }
        return type;
    }

    private void requireActiveMember(UUID groupId, UUID userId) {
        groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);
    }

    private User resolveMember(Group group, UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidExpenseException("Unknown user: " + userId));
        groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(group.getId(), u.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(() -> new InvalidExpenseException(
                        "User " + userId + " is not an active member of the group"));
        return u;
    }
}
