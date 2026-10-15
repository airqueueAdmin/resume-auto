package com.aicareer.platform.retrieval.application;

import java.time.Instant;

public record EmbeddingIndexOutcome(
        int indexedExperienceChunks,
        int indexedProjectChunks,
        String embeddingModel,
        int embeddingDimensions,
        Instant indexedAt
) {
}
