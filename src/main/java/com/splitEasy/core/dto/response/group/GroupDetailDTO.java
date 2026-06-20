package com.splitEasy.core.dto.response.group;

import com.splitEasy.core.dto.response.User.UserSummaryDTO;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.group.GroupInviteLink;
import com.splitEasy.core.entity.group.GroupMembership;
import com.splitEasy.core.enums.GroupRole;
import com.splitEasy.core.enums.GroupType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class GroupDetailDTO {
    private UUID groupId;
    private String name;
    private String description;
    private GroupType type;
    private String defaultCurrencyCode;
    private UserSummaryDTO createdBy;
    private Integer memberCount;
    private GroupRole myRole;
    private List<MemberDTO> members;
    private List<MemberBalanceDTO> memberBalances;
    private List<InviteSummaryDTO> invites;
    private Instant createdAt;
    private Instant updatedAt;

    public static GroupDetailDTO from(Group g, GroupRole myRole,
                                      List<GroupMembership> members,
                                      List<GroupInviteLink> invites) {
        return GroupDetailDTO.builder()
                .groupId(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .type(g.getType())
                .defaultCurrencyCode(g.getDefaultCurrency().getCode())
                .createdBy(UserSummaryDTO.from(g.getCreatedBy()))
                .memberCount(g.getMemberCount())
                .myRole(myRole)
                .members(members.stream().map(MemberDTO::from).toList())
                .memberBalances(List.of())
                .invites(invites.stream().map(InviteSummaryDTO::from).toList())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
