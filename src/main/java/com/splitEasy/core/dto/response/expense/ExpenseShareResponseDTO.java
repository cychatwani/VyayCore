package com.splitEasy.core.dto.response.expense;

import com.splitEasy.core.common.utils.MoneyUtils;
import com.splitEasy.core.entity.expense.ExpenseShare;
import com.splitEasy.core.entity.reference.Currency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ExpenseShareResponseDTO {

    private String userPublicId;
    private Long owedAmountMinor;
    private BigDecimal owedAmount;    // major, for display
    private BigDecimal percentage;    // null unless PERCENTAGE
    private Integer shareWeight;      // null unless SHARES

    public static ExpenseShareResponseDTO from(ExpenseShare s, Currency currency) {
        return ExpenseShareResponseDTO.builder()
                .userPublicId(s.getUser().getPublicId())
                .owedAmountMinor(s.getOwedAmountMinor())
                .owedAmount(MoneyUtils.toMajor(s.getOwedAmountMinor(), currency))
                .percentage(s.getPercentage())
                .shareWeight(s.getShareWeight())
                .build();
    }
}