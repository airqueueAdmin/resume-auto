package com.aicareer.platform.ai.application;

import java.time.Instant;
import java.util.UUID;

public record ResumeDraftOutcome(
        UUID id,
        UUID jobPostingId,
        GroundedResumeDraftDocument draft,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount,
        Instant createdAt
) {
}
