package com.splitEasy.core.entity.settlement;

import com.github.f4b6a3.ulid.Ulid;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.enums.SettlementMethod;
import com.splitEasy.core.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 26)
    private String id;  // ULID

    // Settlements clear GROUP balances, so always group-scoped (never personal).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // Who pays (the debtor settling up).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    // Who receives (the creditor being paid back).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    // Amount transferred, in MINOR units. Always positive.
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PROPOSED;

    // The channel money moved through. Null until known.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SettlementMethod method;

    // Did the app initiate/drive the payment (vs a manually recorded settlement)?
    // App-initiated + rail success -> can auto-confirm; manually recorded -> needs
    // counterparty confirmation (propose -> confirm).
    @Column(name = "app_initiated", nullable = false)
    @Builder.Default
    private boolean appInitiated = false;

    private String note;  // nullable memo

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // Set when status -> CONFIRMED (the moment balance deltas are applied).
    private Instant confirmedAt;  // nullable

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = Ulid.fast().toString();
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }
}