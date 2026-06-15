package com.splitEasy.core.dto.requests.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseShareInputDTO {

    @NotBlank(message = "Participant userPublicId is required")
    private String userPublicId;

    // Exactly one applies, per the expense's splitType (cross-field rule the service
    // enforces, so these stay nullable). Money fields are MAJOR units.
    @PositiveOrZero(message = "exactAmount cannot be negative")
    private BigDecimal exactAmount;   // EXACT

    @Positive(message = "percentage must be positive")
    private BigDecimal percentage;    // PERCENTAGE

    @Positive(message = "shareWeight must be positive")
    private Integer shareWeight;      // SHARES
}