package com.aicareer.platform.identity.application;

import java.time.Duration;
import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.identity.email-verification")
public record EmailVerificationProperties(
        boolean enabled,
        Duration codeTtl,
        Duration resendCooldown,
        int maxAttempts,
        String codeHmacSecretBase64,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword,
        boolean smtpStartTlsEnabled,
        boolean smtpSslEnabled,
        String fromAddress,
        String fromName
) {
    public EmailVerificationProperties {
        if (codeTtl == null || codeTtl.isZero() || codeTtl.isNegative()) {
            throw new IllegalArgumentException("Email verification code TTL must be positive");
        }
        if (resendCooldown == null || resendCooldown.isNegative()) {
            throw new IllegalArgumentException("Email verification resend cooldown must not be negative");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Email verification max attempts must be positive");
        }
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(codeHmacSecretBase64 == null ? "" : codeHmacSecretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Email verification HMAC secret must be Base64-encoded", exception);
        }
        if (secret.length < 32) {
            throw new IllegalArgumentException("Email verification HMAC secret must contain at least 32 decoded bytes");
        }
        if (smtpStartTlsEnabled && smtpSslEnabled) {
            throw new IllegalArgumentException("SMTP STARTTLS and implicit SSL cannot both be enabled");
        }
        if (enabled) {
            requireConfigured(smtpHost, "SMTP host");
            requireConfigured(smtpUsername, "SMTP username");
            requireConfigured(smtpPassword, "SMTP password");
            requireConfigured(fromAddress, "SMTP sender address");
            if (smtpPort < 1 || smtpPort > 65535) {
                throw new IllegalArgumentException("SMTP port must be between 1 and 65535");
            }
        }
    }

    private static void requireConfigured(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be configured when email verification is enabled");
        }
    }
}
