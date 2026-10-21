package com.aicareer.platform.ai.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.self-introduction")
public record SelfIntroductionProperties(
        String promptVersion,
        int topK,
        double minimumSimilarity,
        int maxContextCharacters,
        int maxOutputTokens,
        int geminiThinkingBudget,
        boolean ollamaFallbackEnabled,
        int maxAttempts,
        Duration ollamaTimeout,
        int ollamaMaxOutputTokens
) {
}
