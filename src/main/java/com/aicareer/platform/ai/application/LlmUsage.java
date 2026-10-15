package com.aicareer.platform.ai.application;

public record LlmUsage(
        long inputTokens,
        long outputTokens,
        long totalTokens
) {
}
