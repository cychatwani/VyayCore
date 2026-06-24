package com.vyay.core.dto.response.group;

import com.vyay.core.dto.base.VersionedResponse;
import com.vyay.core.dto.response.User.UserSummaryDTO;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupInviteLink;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.GroupType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Getter
@SuperBuilder
public class GroupDetailDTO extends VersionedResponse {
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

    // Admins first, then members (any future role falls through after).
    // Within each role: case-insensitive alphabetical by displayName.
    private static final Comparator<MemberDTO> MEMBER_ORDER =
            Comparator.comparingInt((MemberDTO m) -> m.getRole() == GroupRole.ADMIN ? 0 : 1)
                    .thenComparing(MemberDTO::getDisplayName, String.CASE_INSENSITIVE_ORDER);

    public static GroupDetailDTO from(Group g, GroupRole myRole,
                                      List<GroupMembership> members,
                                      List<GroupInviteLink> invites,
                                      UUID currentUserId,
                                      String frontendBaseUrl) {
        return GroupDetailDTO.builder()
                .version(g.getVersion())
                .groupId(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .type(g.getType())
                .defaultCurrencyCode(g.getDefaultCurrency().getCode())
                .createdBy(UserSummaryDTO.from(g.getCreatedBy()))
                .memberCount(g.getMemberCount())
                .myRole(myRole)
                .members(members.stream()
                        .map(m -> MemberDTO.from(m, currentUserId))
                        .sorted(MEMBER_ORDER)
                        .toList())
                .memberBalances(List.of())
                .invites(invites.stream().map(i -> InviteSummaryDTO.from(i, frontendBaseUrl)).toList())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}