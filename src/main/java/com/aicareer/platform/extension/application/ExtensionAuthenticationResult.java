package com.aicareer.platform.extension.application;

import java.time.Duration;

import com.aicareer.platform.identity.domain.User;

public record ExtensionAuthenticationResult(
        User user,
        String accessToken,
        Duration accessTokenTtl
) {
}
