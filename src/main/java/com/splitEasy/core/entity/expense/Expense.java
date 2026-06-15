package com.splitEasy.core.entity.expense;

import com.github.f4b6a3.ulid.Ulid;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.enums.SplitType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 26)
    private String id;  // ULID, exposed directly in API

    @Column(nullable = false)
    private String description;

    // Stored in MINOR units of `currency` (e.g. paise for INR). Major value is derived
    // in the response DTO as minor / 10^currency.decimalPlaces.
    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    // Nullable group_id == a PERSONAL (unsplit) expense.
    // Promote-to-split is the first assignment of this FK; we never relocate across groups.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // Who recorded the expense (the actor) - not necessarily who paid.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Null == not split (personal). Set when the expense is split among members.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SplitType splitType;

    // Who actually paid, and how much (supports multiple payers).
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpensePayer> payers = new ArrayList<>();

    // Who the expense is split among, and each one's share (supports multiple participants).
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseShare> shares = new ArrayList<>();

    // When the expense actually occurred - distinct from createdAt (when the row was recorded).
    @Column(nullable = false)
    private Instant expenseDate;

    private String notes;  // nullable

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;  // maps to is_deleted

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
        if (expenseDate == null) {
            expenseDate = Instant.now();
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }
}