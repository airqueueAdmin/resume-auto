package com.aicareer.platform.ai.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.ai.application.GroundedResumeDraftDocument;
import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.ResumeDraftOutcome;

public record ResumeDraftResponse(
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

    public static ResumeDraftResponse from(ResumeDraftOutcome outcome) {
        return new ResumeDraftResponse(
                outcome.id(),
                outcome.jobPostingId(),
                outcome.draft(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.retrievedChunkCount(),
                outcome.createdAt()
        );
    }
}
