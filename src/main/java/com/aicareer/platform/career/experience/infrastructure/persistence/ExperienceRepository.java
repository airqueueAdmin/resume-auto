package com.aicareer.platform.career.experience.infrastructure.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.career.experience.domain.Experience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, UUID> {

    Page<Experience> findAllByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    List<Experience> findAllByUser_IdAndDeletedAtIsNull(UUID userId);

    Optional<Experience> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);
}
