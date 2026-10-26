package com.aicareer.platform.extension.api;

public record ExtensionReadinessResponse(
        long experienceCount,
        long projectCount,
        long indexedEvidenceCount,
        boolean ready,
        String guidance
) {
}
