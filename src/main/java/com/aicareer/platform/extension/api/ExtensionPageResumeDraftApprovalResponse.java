package com.aicareer.platform.extension.api;

import java.util.UUID;

import com.aicareer.platform.career.resume.domain.Resume;

public record ExtensionPageResumeDraftApprovalResponse(
        UUID resumeId,
        String resumeName,
        int versionNumber
) {

    public static ExtensionPageResumeDraftApprovalResponse from(Resume resume) {
        return new ExtensionPageResumeDraftApprovalResponse(
                resume.getId(),
                resume.getName(),
                resume.getLatestVersionNumber()
        );
    }
}
