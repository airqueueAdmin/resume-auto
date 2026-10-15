package com.aicareer.platform.retrieval.api;

import java.util.UUID;

import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.domain.SourceType;

public record VectorSearchResultResponse(
        UUID chunkId,
        SourceType sourceType,
        UUID sourceId,
        int chunkIndex,
        String content,
        double similarity,
        String embeddingModel
) {

    public static VectorSearchResultResponse from(EmbeddingChunkSearchResult result) {
        return new VectorSearchResultResponse(
                result.chunkId(),
                result.sourceType(),
                result.sourceId(),
                result.chunkIndex(),
                result.content(),
                result.similarity(),
                result.embeddingModel()
        );
    }
}
