package com.aicareer.platform.extension.api;

import java.util.List;

import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerOutcome;
import com.aicareer.platform.ai.application.SourceCitation;

public record ExtensionPageAnswerResponse(
        String companyName,
        String jobTitle,
        String sourceUrl,
        String answer,
        String suggestedValue,
        int answerLength,
        Integer maxLength,
        boolean truncated,
        boolean requiresReview,
        boolean persisted,
        List<SourceCitation> citations,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount
) {

    public static ExtensionPageAnswerResponse from(
            ExtensionPageAnswerRequest request,
            String suggestedValue,
            SelfIntroductionAnswerOutcome outcome
    ) {
        return new ExtensionPageAnswerResponse(
                request.companyName().strip(),
                request.jobTitle().strip(),
                request.sourceUrl().strip(),
                outcome.answer(),
                suggestedValue,
                outcome.answer().length(),
                request.maxLength(),
                !outcome.answer().equals(suggestedValue),
                true,
                false,
                outcome.citations(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.retrievedChunkCount()
        );
    }
}
