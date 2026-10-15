package com.aicareer.platform.ai.application;

import java.time.Instant;
import java.util.UUID;

public record JobAnalysisOutcome(
        UUID id,
        UUID jobPostingId,
        JobAnalysisDocument analysis,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        Instant createdAt
) {
}
