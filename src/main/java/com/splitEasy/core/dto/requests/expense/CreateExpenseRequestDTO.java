package com.splitEasy.core.dto.requests.expense;

import com.splitEasy.core.enums.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequestDTO {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    // Major units (e.g. 123.42). Converted to minor in the service using the currency.
    @NotNull(message = "totalAmount is required")
    @Positive(message = "totalAmount must be positive")
    private BigDecimal totalAmount;

    @NotBlank(message = "Currency code is required")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Currency code must be a valid ISO 4217 code format (e.g. USD, INR)"
    )
    private String currencyCode;

    // Null == personal (unsplit) expense.
    private String groupId;

    // Required for a group expense; ignored for personal. Enforced in the service.
    private SplitType splitType;

    // Null -> the entity defaults it to now() on persist.
    private Instant expenseDate;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @Valid
    private List<ExpensePayerInputDTO> payers;

    @Valid
    private List<ExpenseShareInputDTO> participants;
}