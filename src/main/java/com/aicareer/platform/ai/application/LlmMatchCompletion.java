package com.aicareer.platform.ai.application;

public record LlmMatchCompletion(
        JobMatchDocument analysis,
        LlmUsage usage,
        String providerResponseId
) {
}
