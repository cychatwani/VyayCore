package com.splitEasy.core.repository;

import com.splitEasy.core.entity.balance.BalanceLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BalanceLedgerRepository extends JpaRepository<BalanceLedgerEntry, UUID> {

    // Replay a coordinate oldest-first: Σ(deltaMinor) should equal Balance.net.
    List<BalanceLedgerEntry> findByGroupIdAndUserIdAndCurrencyCodeOrderByCreatedAtAsc(
            UUID groupId, UUID userId, String currencyCode);
}
