package com.splitEasy.core.dto.response.expense;

import com.splitEasy.core.common.utils.MoneyUtils;
import com.splitEasy.core.entity.expense.Expense;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.enums.SplitType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ExpenseResponseDTO {

    private String id;
    private String description;
    private Long totalAmountMinor;
    private BigDecimal totalAmount;   // major
    private String currencyCode;
    private String currencySymbol;
    private String groupId;           // null == personal
    private String createdBy;         // creator's public id
    private SplitType splitType;      // null == personal
    private Instant expenseDate;
    private String notes;
    private Instant createdAt;
    private List<ExpensePayerResponseDTO> payers;
    private List<ExpenseShareResponseDTO> shares;

    public static ExpenseResponseDTO from(Expense e) {
        Currency c = e.getCurrency();
        return ExpenseResponseDTO.builder()
                .id(e.getId())
                .description(e.getDescription())
                .totalAmountMinor(e.getTotalAmountMinor())
                .totalAmount(MoneyUtils.toMajor(e.getTotalAmountMinor(), c))
                .currencyCode(c.getCode())
                .currencySymbol(c.getSymbol())
                .groupId(e.getGroup() != null ? e.getGroup().getId() : null)
                .createdBy(e.getCreatedBy().getPublicId())
                .splitType(e.getSplitType())
                .expenseDate(e.getExpenseDate())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .payers(e.getPayers().stream().map(p -> ExpensePayerResponseDTO.from(p, c)).toList())
                .shares(e.getShares().stream().map(s -> ExpenseShareResponseDTO.from(s, c)).toList())
                .build();
    }
}