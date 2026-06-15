package com.splitEasy.core.dto.requests.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePayerInputDTO {

    @NotBlank(message = "Payer userPublicId is required")
    private String userPublicId;

    // Major units (e.g. 123.42). Converted to minor in the service.
    @NotNull(message = "amountPaid is required")
    @Positive(message = "amountPaid must be positive")
    private BigDecimal amountPaid;
}