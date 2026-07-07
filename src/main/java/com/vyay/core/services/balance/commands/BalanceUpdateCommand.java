package com.vyay.core.services.balance.commands;

import com.vyay.core.entity.expense.Expense;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.LedgerSourceType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class BalanceUpdateCommand {

    private final Group group;
    private final Currency currency;
    private final LedgerSourceType sourceType;
    private final UUID sourceId;
    private final Map<UUID, Long> userDeltas;

    private BalanceUpdateCommand(Group group,
                                 Currency currency,
                                 LedgerSourceType sourceType,
                                 UUID sourceId,
                                 Map<UUID, Long> userDeltas) {
        this.group = group;
        this.currency = currency;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.userDeltas = Map.copyOf(userDeltas);
    }

    public static BalanceUpdateCommand from(Expense expense) {
        Map<UUID, Long> userDeltas = new HashMap<>();

        expense.getPayers().forEach(payer ->
                userDeltas.merge(
                        payer.getUser().getId(),
                        payer.getAmountPaidMinor(),
                        Long::sum
                )
        );

        expense.getShares().forEach(share ->
                userDeltas.merge(
                        share.getUser().getId(),
                        -share.getOwedAmountMinor(),
                        Long::sum
                )
        );

        userDeltas.values().removeIf(delta -> delta == 0);

        return new BalanceUpdateCommand(
                expense.getGroup(),
                expense.getCurrency(),
                LedgerSourceType.EXPENSE,
                expense.getId(),
                userDeltas
        );
    }
}