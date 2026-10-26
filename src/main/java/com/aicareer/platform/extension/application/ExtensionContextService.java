package com.aicareer.platform.extension.application;

import java.util.List;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.career.resume.infrastructure.persistence.ResumeVersionRepository;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.application.VectorSearchOutcome;
import com.aicareer.platform.retrieval.application.VectorSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtensionContextService {

    private static final int TOP_K = 8;

    private final ResumeVersionRepository resumeVersionRepository;
    private final VectorSearchService vectorSearchService;

    public ExtensionContextService(
            ResumeVersionRepository resumeVersionRepository,
            VectorSearchService vectorSearchService
    ) {
        this.resumeVersionRepository = resumeVersionRepository;
        this.vectorSearchService = vectorSearchService;
    }

    @Transactional(readOnly = true)
    public ResumeVersion getVersion(UUID userId, UUID versionId) {
        return resumeVersionRepository
                .findByIdAndResume_User_IdAndResume_DeletedAtIsNull(versionId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResumeVersion> listVersions(
            UUID userId,
            org.springframework.data.domain.Pageable pageable
    ) {
        return resumeVersionRepository
                .findAllByResume_User_IdAndResume_DeletedAtIsNull(userId, pageable);
    }

    public List<EmbeddingChunkSearchResult> searchEvidence(UUID userId, String question) {
        VectorSearchOutcome outcome = vectorSearchService.search(userId, question.strip(), TOP_K);
        return outcome.results();
    }
}
