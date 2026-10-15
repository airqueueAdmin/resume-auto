package com.aicareer.platform.ai.infrastructure.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.openai")
public record OpenAiProperties(
        URI endpoint,
        String apiKey,
        String model
) {
}
