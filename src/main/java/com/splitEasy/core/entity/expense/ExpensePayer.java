package com.splitEasy.core.entity.expense;

import com.github.f4b6a3.ulid.Ulid;
import com.splitEasy.core.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "expense_payers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_payer_expense_user",
                columnNames = {"expense_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpensePayer {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 26)
    private String id;  // ULID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Minor units of expense.currency. Sum of all payers == expense.totalAmountMinor.
    @Column(name = "amount_paid_minor", nullable = false)
    private Long amountPaidMinor;

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = Ulid.fast().toString();
        }
    }
}