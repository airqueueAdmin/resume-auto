package com.aicareer.platform.job.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.job.domain.JobPosting;

public record JobPostingResponse(
        UUID id,
        String title,
        String companyName,
        String description,
        String sourceUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static JobPostingResponse from(JobPosting jobPosting) {
        return new JobPostingResponse(
                jobPosting.getId(),
                jobPosting.getTitle(),
                jobPosting.getCompanyName(),
                jobPosting.getDescription(),
                jobPosting.getSourceUrl(),
                jobPosting.getCreatedAt(),
                jobPosting.getUpdatedAt()
        );
    }
}
