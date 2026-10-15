package com.aicareer.platform.ai.infrastructure.persistence;

import java.util.UUID;
import java.util.Optional;

import com.aicareer.platform.ai.domain.ResumeDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeDraftRepository extends JpaRepository<ResumeDraft, UUID> {

    Optional<ResumeDraft> findByIdAndUser_Id(UUID id, UUID userId);
}
