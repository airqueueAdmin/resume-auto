package com.aicareer.platform.identity.api;

public record SignupPolicyResponse(
        String termsVersion,
        String privacyVersion,
        boolean emailVerificationRequired
) {
}
