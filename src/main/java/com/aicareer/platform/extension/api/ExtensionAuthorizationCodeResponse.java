package com.aicareer.platform.extension.api;

import java.time.Duration;
import java.time.Instant;

import com.aicareer.platform.extension.application.ExtensionAuthorizationService.AuthorizationCode;

public record ExtensionAuthorizationCodeResponse(
        String code,
        Instant expiresAt,
        long expiresInSeconds
) {

    public static ExtensionAuthorizationCodeResponse from(AuthorizationCode authorizationCode, Instant now) {
        Duration remaining = Duration.between(now, authorizationCode.expiresAt());
        return new ExtensionAuthorizationCodeResponse(
                authorizationCode.value(),
                authorizationCode.expiresAt(),
                Math.max(0, remaining.toSeconds())
        );
    }
}
