package com.aicareer.platform.extension.application;

import java.util.UUID;

import com.aicareer.platform.career.experience.infrastructure.persistence.ExperienceRepository;
import com.aicareer.platform.career.project.infrastructure.persistence.ProjectRepository;
import com.aicareer.platform.extension.api.ExtensionReadinessResponse;
import com.aicareer.platform.retrieval.infrastructure.persistence.EmbeddingChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtensionReadinessService {

    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final EmbeddingChunkRepository embeddingChunkRepository;

    public ExtensionReadinessService(
            ExperienceRepository experienceRepository,
            ProjectRepository projectRepository,
            EmbeddingChunkRepository embeddingChunkRepository
    ) {
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.embeddingChunkRepository = embeddingChunkRepository;
    }

    @Transactional(readOnly = true)
    public ExtensionReadinessResponse get(UUID userId) {
        long experienceCount = experienceRepository.countByUser_IdAndDeletedAtIsNull(userId);
        long projectCount = projectRepository.countByUser_IdAndDeletedAtIsNull(userId);
        long indexedEvidenceCount = embeddingChunkRepository.countActiveForUser(userId);
        boolean hasCareerData = experienceCount + projectCount > 0;
        boolean ready = hasCareerData && indexedEvidenceCount > 0;
        String guidance = !hasCareerData
                ? "서비스에서 경력 또는 프로젝트를 먼저 등록해주세요."
                : indexedEvidenceCount == 0
                        ? "서비스 커리어 메뉴에서 맞춤 검색 데이터를 업데이트해주세요."
                        : "경력 근거가 준비되었습니다.";
        return new ExtensionReadinessResponse(
                experienceCount,
                projectCount,
                indexedEvidenceCount,
                ready,
                guidance
        );
    }
}
