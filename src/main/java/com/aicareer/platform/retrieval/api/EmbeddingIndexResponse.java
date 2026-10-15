package com.aicareer.platform.retrieval.api;

import java.time.Instant;

import com.aicareer.platform.retrieval.application.EmbeddingIndexOutcome;

public record EmbeddingIndexResponse(
        int indexedExperienceChunks,
        int indexedProjectChunks,
        String embeddingModel,
        int embeddingDimensions,
        Instant indexedAt
) {

    public static EmbeddingIndexResponse from(EmbeddingIndexOutcome outcome) {
        return new EmbeddingIndexResponse(
                outcome.indexedExperienceChunks(),
                outcome.indexedProjectChunks(),
                outcome.embeddingModel(),
                outcome.embeddingDimensions(),
                outcome.indexedAt()
        );
    }
}
