package com.aicareer.platform.ai.application;

import java.util.List;

public record SelfIntroductionAnswerOutcome(
        String answer,
        List<SourceCitation> citations,
        String provider,
        String model,
        String promptVersion,
        LlmUsage usage,
        int retrievedChunkCount
) {
    public SelfIntroductionAnswerOutcome {
        citations = List.copyOf(citations == null ? List.of() : citations);
    }
}
