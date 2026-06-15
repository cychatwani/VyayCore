package com.splitEasy.core.repository;

import com.splitEasy.core.entity.balance.BalanceLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceLedgerRepository extends JpaRepository<BalanceLedgerEntry, String> {

    // Replay a coordinate oldest-first: Σ(deltaMinor) should equal Balance.net.
    List<BalanceLedgerEntry> findByGroupIdAndUserIdAndCurrencyCodeOrderByCreatedAtAsc(
            String groupId, Long userId, String currencyCode);
}