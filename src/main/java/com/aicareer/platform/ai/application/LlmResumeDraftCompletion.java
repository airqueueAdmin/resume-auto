package com.aicareer.platform.ai.application;

public record LlmResumeDraftCompletion(
        ResumeDraftDocument draft,
        LlmUsage usage,
        String providerResponseId
) {
}
