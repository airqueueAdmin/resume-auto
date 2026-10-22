package com.aicareer.platform.identity.application;

import java.time.Instant;

import com.aicareer.platform.identity.domain.User;

public record RegistrationResult(
        User user,
        boolean verificationRequired,
        Instant verificationExpiresAt
) {
}
