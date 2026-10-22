package com.aicareer.platform.identity.api;

import java.time.Instant;

import com.aicareer.platform.identity.application.RegistrationResult;

public record SignupResponse(
        String email,
        boolean verificationRequired,
        Instant verificationExpiresAt
) {
    public static SignupResponse from(RegistrationResult result) {
        return new SignupResponse(
                result.user().getEmail(),
                result.verificationRequired(),
                result.verificationExpiresAt()
        );
    }
}
