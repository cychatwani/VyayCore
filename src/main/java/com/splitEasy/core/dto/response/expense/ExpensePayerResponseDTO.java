package com.splitEasy.core.dto.response.expense;

import com.splitEasy.core.common.utils.MoneyUtils;
import com.splitEasy.core.entity.expense.ExpensePayer;
import com.splitEasy.core.entity.reference.Currency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ExpensePayerResponseDTO {

    private String userPublicId;
    private Long amountPaidMinor;
    private BigDecimal amountPaid;   // major, for display

    public static ExpensePayerResponseDTO from(ExpensePayer p, Currency currency) {
        return ExpensePayerResponseDTO.builder()
                .userPublicId(p.getUser().getPublicId())
                .amountPaidMinor(p.getAmountPaidMinor())
                .amountPaid(MoneyUtils.toMajor(p.getAmountPaidMinor(), currency))
                .build();
    }
}