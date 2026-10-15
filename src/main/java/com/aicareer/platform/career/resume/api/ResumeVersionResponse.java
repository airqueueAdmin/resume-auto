package com.aicareer.platform.career.resume.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.ResumeVersion;

public record ResumeVersionResponse(
        UUID id,
        UUID resumeId,
        int versionNumber,
        String content,
        Instant createdAt
) {

    public static ResumeVersionResponse from(ResumeVersion version) {
        return new ResumeVersionResponse(
                version.getId(),
                version.getResumeId(),
                version.getVersionNumber(),
                version.getContent(),
                version.getCreatedAt()
        );
    }
}
