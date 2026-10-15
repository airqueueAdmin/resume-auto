package com.aicareer.platform.identity.api;

import com.aicareer.platform.identity.application.AuthenticationResult;

public record AuthenticationResponse(
        UserResponse user,
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds
) {

    public static AuthenticationResponse from(AuthenticationResult result) {
        return new AuthenticationResponse(
                UserResponse.from(result.user()),
                "Bearer",
                result.accessToken(),
                result.accessTokenTtl().toSeconds(),
                result.refreshToken(),
                result.refreshTokenTtl().toSeconds()
        );
    }
}
