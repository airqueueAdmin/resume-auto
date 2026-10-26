package com.aicareer.platform.ai.application;

import java.time.Instant;

public record PageResumeDraftOutcome(
        GroundedResumeDraftDocument draft,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount,
        Instant generatedAt
) {
}
