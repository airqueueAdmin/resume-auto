package com.aicareer.platform.career.resume.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.Resume;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Page<Resume> findAllByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Optional<Resume> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select resume
            from Resume resume
            where resume.id = :resumeId
              and resume.user.id = :userId
              and resume.deletedAt is null
            """)
    Optional<Resume> findForUpdate(
            @Param("resumeId") UUID resumeId,
            @Param("userId") UUID userId
    );
}
