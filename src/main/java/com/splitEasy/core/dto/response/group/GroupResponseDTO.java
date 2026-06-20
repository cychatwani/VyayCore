package com.splitEasy.core.dto.response.group;

import com.splitEasy.core.dto.response.User.UserSummaryDTO;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.enums.GroupType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class GroupResponseDTO {
    private UUID groupId;
    private String name;
    private String description;
    private GroupType type;
    private String defaultCurrencyCode;
    private UserSummaryDTO createdBy;
    private Integer memberCount;
    private Instant createdAt;

    public static GroupResponseDTO from(Group group, User creator) {
        return GroupResponseDTO.builder()
                .groupId(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .type(group.getType())
                .defaultCurrencyCode(group.getDefaultCurrency().getCode())
                .createdBy(UserSummaryDTO.from(creator))
                .memberCount(group.getMemberCount())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
