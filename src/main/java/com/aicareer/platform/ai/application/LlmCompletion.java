package com.aicareer.platform.ai.application;

public record LlmCompletion(
        JobAnalysisDocument analysis,
        LlmUsage usage,
        String providerResponseId
) {
}
