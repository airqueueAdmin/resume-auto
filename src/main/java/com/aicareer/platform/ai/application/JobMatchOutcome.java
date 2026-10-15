package com.aicareer.platform.ai.application;

import java.time.Instant;
import java.util.UUID;

public record JobMatchOutcome(
        UUID id,
        UUID jobPostingId,
        GroundedJobMatchDocument analysis,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount,
        Instant createdAt
) {
}
