package com.aicareer.platform.career.resume.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.ResumeVersion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {

    Page<ResumeVersion> findAllByResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(
            UUID resumeId,
            UUID userId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "resume")
    Page<ResumeVersion> findAllByResume_User_IdAndResume_DeletedAtIsNull(
            UUID userId,
            Pageable pageable
    );

    Optional<ResumeVersion> findByIdAndResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(
            UUID versionId,
            UUID resumeId,
            UUID userId
    );

    @EntityGraph(attributePaths = "resume")
    Optional<ResumeVersion> findByIdAndResume_User_IdAndResume_DeletedAtIsNull(
            UUID versionId,
            UUID userId
    );
}
