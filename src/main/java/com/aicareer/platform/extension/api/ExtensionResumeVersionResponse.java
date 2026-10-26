package com.aicareer.platform.extension.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.ResumeVersion;

public record ExtensionResumeVersionResponse(
        UUID versionId,
        UUID resumeId,
        String resumeName,
        int versionNumber,
        Instant createdAt
) {

    public static ExtensionResumeVersionResponse from(ResumeVersion version) {
        return new ExtensionResumeVersionResponse(
                version.getId(),
                version.getResumeId(),
                version.getResumeName(),
                version.getVersionNumber(),
                version.getCreatedAt()
        );
    }
}
