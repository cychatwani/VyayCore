package com.splitEasy.core.entity.balance;

import com.github.f4b6a3.ulid.Ulid;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.reference.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_balance_group_user_currency",
                columnNames = {"group_id", "user_id", "currency_code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 26)
    private String id;  // ULID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    // Signed net position in MINOR units within (group, currency):
    //   > 0  -> the user is owed this much (net creditor)
    //   < 0  -> the user owes this much (net debtor)
    // Sum of all members' net within a (group, currency) is always 0.
    @Column(name = "net_amount_minor", nullable = false)
    @Builder.Default
    private Long netAmountMinor = 0L;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

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