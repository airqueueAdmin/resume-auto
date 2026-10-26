package com.aicareer.platform.ai.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.resume-draft")
public record ResumeDraftProperties(
        String provider,
        String promptVersion,
        int topK,
        int maxContextCharacters,
        int maxOutputTokens,
        int geminiThinkingBudget,
        int maxAttempts,
        boolean ollamaFallbackEnabled,
        Duration ollamaTimeout
) {
}
