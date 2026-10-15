package com.aicareer.platform.identity.application;

import java.time.Duration;

import com.aicareer.platform.identity.domain.User;

public record AuthenticationResult(
        User user,
        String accessToken,
        String refreshToken,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
