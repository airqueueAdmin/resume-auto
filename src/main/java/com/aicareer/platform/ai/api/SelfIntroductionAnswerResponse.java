package com.aicareer.platform.ai.api;

import java.util.List;

import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerOutcome;
import com.aicareer.platform.ai.application.SourceCitation;

public record SelfIntroductionAnswerResponse(
        String answer,
        List<SourceCitation> citations,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount,
        boolean diagnosticsVisible
) {

    public static SelfIntroductionAnswerResponse from(SelfIntroductionAnswerOutcome outcome, boolean diagnosticsVisible) {
        return new SelfIntroductionAnswerResponse(
                outcome.answer(),
                outcome.citations(),
                outcome.provider(),
                outcome.model(),
                outcome.promptVersion(),
                outcome.usage(),
                outcome.retrievedChunkCount(),
                diagnosticsVisible
        );
    }
}
