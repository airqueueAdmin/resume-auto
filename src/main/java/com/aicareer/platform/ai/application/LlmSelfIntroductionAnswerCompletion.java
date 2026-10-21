package com.aicareer.platform.ai.application;

public record LlmSelfIntroductionAnswerCompletion(
        SelfIntroductionAnswerDocument answer,
        LlmUsage usage,
        String providerResponseId
) {
}
