package com.aicareer.platform.ai.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.ai.application.JobAnalysisOutcome;
import com.aicareer.platform.ai.application.JobAnalysisDocument;
import com.aicareer.platform.ai.application.LlmUsage;

public record JobAnalysisResponse(
        UUID id,
        UUID jobPostingId,
        JobAnalysisDocument analysis,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        Instant createdAt
) {

    public static JobAnalysisResponse from(JobAnalysisOutcome outcome) {
        return new JobAnalysisResponse(
                outcome.id(),
                outcome.jobPostingId(),
                outcome.analysis(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.createdAt()
        );
    }
}
