package com.aicareer.platform.ai.application;

import java.util.UUID;

import com.aicareer.platform.retrieval.domain.SourceType;

public record SourceCitation(
        String citationId,
        UUID chunkId,
        SourceType sourceType,
        UUID sourceId,
        int chunkIndex,
        String content,
        double similarity,
        String embeddingModel
) {
}
