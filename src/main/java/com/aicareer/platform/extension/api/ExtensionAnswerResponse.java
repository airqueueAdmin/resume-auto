package com.aicareer.platform.extension.api;

import java.util.List;
import java.util.UUID;

import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerOutcome;
import com.aicareer.platform.ai.application.SourceCitation;
import com.aicareer.platform.career.resume.domain.ResumeVersion;

public record ExtensionAnswerResponse(
        UUID resumeVersionId,
        String resumeName,
        int versionNumber,
        UUID jobPostingId,
        String answer,
        String suggestedValue,
        int answerLength,
        Integer maxLength,
        boolean truncated,
        boolean requiresReview,
        List<SourceCitation> citations,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount
) {

    public static ExtensionAnswerResponse from(
            ResumeVersion version,
            UUID jobPostingId,
            Integer maxLength,
            String suggestedValue,
            SelfIntroductionAnswerOutcome outcome
    ) {
        return new ExtensionAnswerResponse(
                version.getId(),
                version.getResumeName(),
                version.getVersionNumber(),
                jobPostingId,
                outcome.answer(),
                suggestedValue,
                outcome.answer().length(),
                maxLength,
                !outcome.answer().equals(suggestedValue),
                true,
                outcome.citations(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.retrievedChunkCount()
        );
    }
}
