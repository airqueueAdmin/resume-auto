package com.aicareer.platform.career.project.infrastructure.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.career.project.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findAllByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    List<Project> findAllByUser_IdAndDeletedAtIsNull(UUID userId);

    long countByUser_IdAndDeletedAtIsNull(UUID userId);

    Optional<Project> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);
}
