package com.splitEasy.core.services.expense;

import com.splitEasy.core.common.security.HmacSigner;
import com.splitEasy.core.common.utils.MoneyUtils;
import com.splitEasy.core.dto.requests.expense.CreateExpenseRequestDTO;
import com.splitEasy.core.dto.requests.expense.ExpensePayerInputDTO;
import com.splitEasy.core.dto.requests.expense.ExpenseShareInputDTO;
import com.splitEasy.core.dto.response.expense.ExpenseResponseDTO;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.balance.BalanceLedgerEntry;
import com.splitEasy.core.entity.expense.Expense;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.enums.LedgerSourceType;
import com.splitEasy.core.enums.MembershipStatus;
import com.splitEasy.core.enums.SplitType;
import com.splitEasy.core.exception.business.GroupNotFoundException;
import com.splitEasy.core.exception.business.InvalidCurrencyException;
import com.splitEasy.core.exception.business.InvalidExpenseException;
import com.splitEasy.core.exception.business.NotAMemberException;
import com.splitEasy.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.ulid.Ulid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        // Currency support check == presence in the table (no isActive flag).
        Currency currency = currencyRepository.findById(request.getCurrencyCode())
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
                .expenseDate(request.getExpenseDate())  // null -> @PrePersist defaults to now
                .notes(request.getNotes())
                .build();

        if (group == null) {
            // Personal: creator bears the whole thing; any payers/participants are ignored.
            expense.getPayers().add(lineFactory.payer(expense, principal, total));
            expense.getShares().add(lineFactory.share(expense, principal, total, null, null));
        } else {
            buildPayers(expense, group, request.getPayers(), total, currency);
            buildShares(expense, group, request.getSplitType(), request.getParticipants(), total, currency);
        }

        Expense saved = expenseRepository.save(expense);   // assigns id, persists lines

        if (saved.getGroup() != null) {
            applyBalanceDeltas(saved);                     // id now available as ledger sourceId
        }

        return ExpenseResponseDTO.from(saved);
    }

    // Group expenses only: bump each involved user's net balance by (paid - owed),
    // and append a tamper-evident ledger entry for the same delta.
    private void applyBalanceDeltas(Expense expense) {
        String groupId = expense.getGroup().getId();
        String currencyCode = expense.getCurrency().getCode();

        Map<Long, Long> netByUser = new HashMap<>();
        for (var p : expense.getPayers()) {
            netByUser.merge(p.getUser().getId(), p.getAmountPaidMinor(), Long::sum);
        }
        for (var s : expense.getShares()) {
            netByUser.merge(s.getUser().getId(), -s.getOwedAmountMinor(), Long::sum);
        }

        netByUser.forEach((userId, delta) -> {
            if (delta == 0) {
                return;  // paid exactly their share - no movement, no ledger entry
            }
            // 1. running projection (lock-free atomic upsert)
            balanceRepository.applyDelta(Ulid.fast().toString(), groupId, userId, currencyCode, delta);

            // 2. append-only signed audit entry
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
        Set<String> seen = new HashSet<>();
        long sum = 0;
        for (ExpensePayerInputDTO in : inputs) {
            if (!seen.add(in.getUserPublicId())) {
                throw new InvalidExpenseException("Duplicate payer: " + in.getUserPublicId());
            }
            long paid = MoneyUtils.toMinor(in.getAmountPaid(), currency);
            if (paid <= 0) {
                throw new InvalidExpenseException("Each payer must have a positive amount");
            }
            expense.getPayers().add(lineFactory.payer(expense, resolveMember(group, in.getUserPublicId()), paid));
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
        Set<String> seen = new HashSet<>();
        List<User> users = new ArrayList<>(inputs.size());
        for (ExpenseShareInputDTO in : inputs) {
            if (!seen.add(in.getUserPublicId())) {
                throw new InvalidExpenseException("Duplicate participant: " + in.getUserPublicId());
            }
            users.add(resolveMember(group, in.getUserPublicId()));
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

    private void requireActiveMember(String groupId, Long userId) {
        groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);
    }

    private User resolveMember(Group group, String userPublicId) {
        User u = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new InvalidExpenseException("Unknown user: " + userPublicId));
        groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(group.getId(), u.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(() -> new InvalidExpenseException(
                        "User " + userPublicId + " is not an active member of the group"));
        return u;
    }
}