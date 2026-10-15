package com.aicareer.platform.career.resume.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.Resume;

public record ResumeResponse(
        UUID id,
        String name,
        int latestVersionNumber,
        Instant createdAt,
        Instant updatedAt
) {

    public static ResumeResponse from(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getName(),
                resume.getLatestVersionNumber(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
