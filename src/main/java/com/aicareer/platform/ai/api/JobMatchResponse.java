package com.aicareer.platform.ai.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.ai.application.GroundedJobMatchDocument;
import com.aicareer.platform.ai.application.JobMatchOutcome;
import com.aicareer.platform.ai.application.LlmUsage;

public record JobMatchResponse(
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

    public static JobMatchResponse from(JobMatchOutcome outcome) {
        return new JobMatchResponse(
                outcome.id(),
                outcome.jobPostingId(),
                outcome.analysis(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.retrievedChunkCount(),
                outcome.createdAt()
        );
    }
}
