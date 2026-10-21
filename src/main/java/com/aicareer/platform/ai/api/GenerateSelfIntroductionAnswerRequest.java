package com.aicareer.platform.ai.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record GenerateSelfIntroductionAnswerRequest(
        @NotNull UUID resumeId,
        @NotNull UUID questionId
) {
}
