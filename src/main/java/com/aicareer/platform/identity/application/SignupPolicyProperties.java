package com.aicareer.platform.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.identity.signup")
public record SignupPolicyProperties(
        String termsVersion,
        String privacyVersion
) {
    public SignupPolicyProperties {
        if (termsVersion == null || termsVersion.isBlank()) {
            throw new IllegalArgumentException("Signup terms version must not be blank");
        }
        if (privacyVersion == null || privacyVersion.isBlank()) {
            throw new IllegalArgumentException("Signup privacy version must not be blank");
        }
    }
}
