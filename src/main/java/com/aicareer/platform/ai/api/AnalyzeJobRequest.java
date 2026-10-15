package com.aicareer.platform.ai.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AnalyzeJobRequest(
        @NotNull UUID jobPostingId
) {
}
