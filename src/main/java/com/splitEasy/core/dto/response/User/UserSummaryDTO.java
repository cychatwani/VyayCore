package com.splitEasy.core.dto.response.User;

import com.splitEasy.core.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserSummaryDTO {
    private UUID userId;
    private String displayName;

    public static UserSummaryDTO from(User user) {
        return UserSummaryDTO.builder()
                .userId(user.getId())
                .displayName(user.getFullName())
                .build();
    }
}