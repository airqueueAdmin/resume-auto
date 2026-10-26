package com.aicareer.platform.extension.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.domain.SourceType;

public record ExtensionContextResponse(
        ResumeVersionContext resumeVersion,
        List<Evidence> evidence
) {

    public static ExtensionContextResponse from(ResumeVersion version, List<EmbeddingChunkSearchResult> results) {
        return new ExtensionContextResponse(
                ResumeVersionContext.from(version),
                results.stream().map(Evidence::from).toList()
        );
    }

    public record ResumeVersionContext(
            UUID versionId,
            UUID resumeId,
            String resumeName,
            int versionNumber,
            String content,
            Instant createdAt,
            Provenance provenance
    ) {

        public static ResumeVersionContext from(ResumeVersion version) {
            return new ResumeVersionContext(
                    version.getId(),
                    version.getResumeId(),
                    version.getResumeName(),
                    version.getVersionNumber(),
                    version.getContent(),
                    version.getCreatedAt(),
                    new Provenance("RESUME_VERSION", version.getId(), "User-selected immutable ResumeVersion")
            );
        }
    }

    public record Provenance(String sourceType, UUID sourceId, String description) {
    }

    public record Evidence(
            UUID chunkId,
            SourceType sourceType,
            UUID sourceId,
            int chunkIndex,
            String content,
            double similarity,
            String embeddingModel
    ) {

        public static Evidence from(EmbeddingChunkSearchResult result) {
            return new Evidence(
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
}
