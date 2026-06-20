package com.splitEasy.core.dto.response.group;

import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.GroupMembership;
import com.splitEasy.core.enums.GroupRole;
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

    public static MemberDTO from(GroupMembership m) {
        User u = m.getUser();
        return MemberDTO.builder()
                .userId(u.getId())
                .displayName(u.getFullName())
                .profilePicture(u.getProfilePicture())
                .role(m.getRole())
                .activeSince(m.getActiveSince())
                .build();
    }
}
