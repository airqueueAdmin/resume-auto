package com.aicareer.platform.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.data-encryption")
public record DataEncryptionProperties(String keyBase64) {
}
