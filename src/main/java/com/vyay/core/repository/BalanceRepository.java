package com.vyay.core.repository;

import com.vyay.core.entity.balance.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    /**
     * Atomically applies a signed delta to (group, user, currency), inserting the row
     * on first touch. Postgres UPSERT - race-safe under concurrent expense creation,
     * since the read-modify-write happens inside a single statement on the DB.
     * Bypasses JPA lifecycle by design; created_at/updated_at use DB time here.
     *
     * NOTE: currency_code on this table is the Currency PK (UUID), NOT the ISO code
     * — Currency moved to a UUID PK in the base-entity migration. The column kept its
     * historical name; only the type changed.
     */
    @Modifying
    @Query(value = """
            INSERT INTO balances (id, group_id, user_id, currency_code, net_amount_minor, created_at, updated_at)
            VALUES (:id, :groupId, :userId, :currencyId, :delta, now(), now())
            ON CONFLICT (group_id, user_id, currency_code)
            DO UPDATE SET net_amount_minor = balances.net_amount_minor + EXCLUDED.net_amount_minor,
                          updated_at = now()
            """, nativeQuery = true)
    void applyDelta(@Param("id") UUID id,
                    @Param("groupId") UUID groupId,
                    @Param("userId") UUID userId,
                    @Param("currencyId") UUID currencyId,
                    @Param("delta") long delta);

    Optional<Balance> findByGroupIdAndUserIdAndCurrencyId(UUID groupId, UUID userId, UUID currencyId);
}