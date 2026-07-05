    package com.vyay.core.services.balance;

    import com.github.f4b6a3.uuid.UuidCreator;
    import com.vyay.core.common.security.HmacSigner;
    import com.vyay.core.entity.balance.BalanceLedgerEntry;
    import com.vyay.core.entity.expense.Expense;
    import com.vyay.core.enums.LedgerSourceType;
    import com.vyay.core.repository.BalanceLedgerRepository;
    import com.vyay.core.repository.BalanceRepository;
    import com.vyay.core.repository.UserRepository;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.stereotype.Service;

    import java.util.*;

    @Service
    public class BalanceUpdateService {

        private final BalanceRepository balanceRepository;
        private final BalanceLedgerRepository ledgerRepository;
        private final UserRepository userRepository;
        private final HmacSigner hmacSigner;

        public BalanceUpdateService(BalanceRepository balanceRepository,
                                    BalanceLedgerRepository ledgerRepository,
                                    UserRepository userRepository,
                                    HmacSigner hmacSigner) {
            this.balanceRepository = balanceRepository;
            this.ledgerRepository = ledgerRepository;
            this.userRepository = userRepository;
            this.hmacSigner = hmacSigner;
        }

        @Transactional
        public void applyDeltas(Expense expense) {
            UUID groupId = expense.getGroup().getId();
            UUID currencyId = expense.getCurrency().getId();
        
            Map<UUID, Long> netByUser = new HashMap<>();
            for (var payer : expense.getPayers())
                netByUser.merge(payer.getUser().getId(), payer.getAmountPaidMinor(), Long::sum);
            for (var share : expense.getShares())
                netByUser.merge(share.getUser().getId(), -share.getOwedAmountMinor(), Long::sum);
        
            // drop zero-nets, materialize the surviving entries once
            var entries = netByUser.entrySet().stream()
                    .filter(e -> e.getValue() != 0)
                    .toList();
            if (entries.isEmpty()) return;
        
            int n = entries.size();
            UUID[] ids = new UUID[n], groupIds = new UUID[n], userIds = new UUID[n], currencyIds = new UUID[n];
            Long[] deltas = new Long[n];
            List<BalanceLedgerEntry> ledgerEntries = new ArrayList<>(n);
        
            for (int i = 0; i < n; i++) {
                var e = entries.get(i);
                ids[i] = UuidCreator.getTimeOrderedEpoch();
                groupIds[i] = groupId;
                userIds[i] = e.getKey();
                currencyIds[i] = currencyId;
                deltas[i] = e.getValue();

                ledgerEntries.add(hmacSigner.signedEntry(
                        expense.getGroup(),
                        userRepository.getReferenceById(e.getKey()),   // proxy — .getId() won't trigger a load
                        expense.getCurrency(),
                        e.getValue(),
                        LedgerSourceType.EXPENSE,
                        expense.getId()));
            }
        
            balanceRepository.applyDeltasBatch(ids, groupIds, userIds, currencyIds, deltas);
            ledgerRepository.saveAll(ledgerEntries);
        }    
    }