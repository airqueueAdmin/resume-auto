package com.aicareer.platform.ai.infrastructure.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.ollama")
public record OllamaProperties(
        URI endpoint,
        String model
) {
}
