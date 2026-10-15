package com.aicareer.platform.retrieval.application;

import java.util.UUID;

import com.aicareer.platform.retrieval.domain.SourceType;

public record EmbeddingChunkSearchResult(
        UUID chunkId,
        SourceType sourceType,
        UUID sourceId,
        int chunkIndex,
        String content,
        double similarity,
        String embeddingModel
) {
}
