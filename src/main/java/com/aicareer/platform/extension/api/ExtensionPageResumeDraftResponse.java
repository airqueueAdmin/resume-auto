package com.aicareer.platform.extension.api;

import java.time.Instant;

import com.aicareer.platform.ai.application.GroundedResumeDraftDocument;
import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.PageResumeDraftOutcome;

public record ExtensionPageResumeDraftResponse(
        String companyName,
        String jobTitle,
        String sourceUrl,
        int jobDescriptionLength,
        GroundedResumeDraftDocument draft,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount,
        Instant generatedAt,
        boolean persisted
) {

    public static ExtensionPageResumeDraftResponse from(
            ExtensionPageResumeDraftRequest request,
            PageResumeDraftOutcome outcome
    ) {
        return new ExtensionPageResumeDraftResponse(
                request.companyName().strip(),
                request.jobTitle().strip(),
                request.sourceUrl().strip(),
                request.jobDescription().strip().length(),
                outcome.draft(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.retrievedChunkCount(),
                outcome.generatedAt(),
                false
        );
    }
}
