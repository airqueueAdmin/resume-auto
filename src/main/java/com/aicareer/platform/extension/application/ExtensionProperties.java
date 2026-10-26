package com.aicareer.platform.extension.application;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.extension")
public record ExtensionProperties(
        Duration authorizationCodeTtl,
        List<String> allowedOriginPatterns
) {
}
