package com.aicareer.platform.retrieval.application;

import java.util.List;

public record VectorSearchOutcome(
        List<EmbeddingChunkSearchResult> results,
        String embeddingModel,
        int embeddingDimensions
) {
}
