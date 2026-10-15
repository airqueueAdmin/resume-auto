package com.aicareer.platform.retrieval.api;

import java.util.List;

import com.aicareer.platform.retrieval.application.VectorSearchOutcome;

public record VectorSearchResponse(
        List<VectorSearchResultResponse> results,
        String embeddingModel,
        int embeddingDimensions
) {

    public static VectorSearchResponse from(VectorSearchOutcome outcome) {
        return new VectorSearchResponse(
                outcome.results().stream()
                        .map(VectorSearchResultResponse::from)
                        .toList(),
                outcome.embeddingModel(),
                outcome.embeddingDimensions()
        );
    }
}
