package com.splitEasy.core.dto.response.group;

import com.splitEasy.core.dto.balance.BalanceDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MemberBalanceDTO {
    private UUID userId;
    private List<BalanceDTO> balances;
}
