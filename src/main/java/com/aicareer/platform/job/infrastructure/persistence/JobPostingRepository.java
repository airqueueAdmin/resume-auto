package com.aicareer.platform.job.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.job.domain.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    Page<JobPosting> findAllByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Optional<JobPosting> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);
}
