package com.splitEasy.core.dto.response.group;

import com.splitEasy.core.dto.balance.BalanceDTO;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.enums.GroupType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class GroupSummaryDTO {
    private UUID groupId;
    private String name;
    private GroupType type;
    private String defaultCurrencyCode;
    private Integer memberCount;
    private List<BalanceDTO> myBalance;
    private Instant createdAt;

    public static GroupSummaryDTO from(Group g) {
        return GroupSummaryDTO.builder()
                .groupId(g.getId())
                .name(g.getName())
                .type(g.getType())
                .defaultCurrencyCode(g.getDefaultCurrency().getCode())
                .memberCount(g.getMemberCount())
                .myBalance(List.of())
                .createdAt(g.getCreatedAt())
                .build();
    }
}
