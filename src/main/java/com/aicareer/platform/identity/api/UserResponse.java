package com.aicareer.platform.identity.api;

import java.util.UUID;

import com.aicareer.platform.identity.domain.User;

public record UserResponse(
        UUID id,
        String email,
        String displayName
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
