package com.splitEasy.core.entity.balance;

import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.enums.LedgerSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "balance_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceLedgerEntry {

    // All fields are updatable=false: ledger entries are append-only and never mutated.
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 26)
    private String id;  // ULID - assigned before signing, part of the signed payload

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false, updatable = false)
    private Currency currency;

    // The signed delta applied to the running balance (minor units, signed).
    @Column(name = "delta_minor", nullable = false, updatable = false)
    private Long deltaMinor;

    // What caused this entry - lets settlements share the ledger later.
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false, length = 20)
    private LedgerSourceType sourceType;

    @Column(name = "source_id", nullable = false, updatable = false, length = 26)
    private String sourceId;  // e.g. the expense id

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;  // assigned before signing, part of the signed payload

    // HMAC-SHA256 of the canonical payload, hex-encoded (64 chars). Tamper-evidence.
    @Column(name = "hmac", nullable = false, updatable = false, length = 64)
    private String hmac;
}