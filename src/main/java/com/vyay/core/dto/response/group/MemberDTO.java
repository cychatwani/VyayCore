package com.vyay.core.dto.response.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vyay.core.entity.User;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.enums.GroupRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MemberDTO {
    private UUID userId;
    private String displayName;
    private String profilePicture;
    private GroupRole role;
    private Instant activeSince;

    @Getter(onMethod_ = @JsonProperty("isCurrentUser"))
    private boolean isCurrentUser;

    /**
     * Context-free mapping. isCurrentUser defaults to false — only safe when the
     * caller has no "viewer" notion (e.g. webhook payloads, admin tools).
     * For requests on behalf of a user, use {@link #from(GroupMembership, UUID)}.
     */
    public static MemberDTO from(GroupMembership m) {
        return from(m, null);
    }

    /**
     * Viewer-aware mapping: marks isCurrentUser=true when the membership's user
     * matches the supplied viewer id.
     */
    public static MemberDTO from(GroupMembership m, UUID currentUserId) {
        User u = m.getUser();
        return MemberDTO.builder()
                .userId(u.getId())
                .displayName(u.getFullName())
                .profilePicture(u.getProfilePicture())
                .role(m.getRole())
                .activeSince(m.getActiveSince())
                .isCurrentUser(currentUserId != null && currentUserId.equals(u.getId()))
                .build();
    }
}