package com.aicareer.platform.job.api;

import java.util.List;

public record JobPostingImportPreview(
        String sourceUrl,
        String title,
        String companyName,
        String description,
        String extractionSource,
        List<String> warnings,
        List<String> imageUrls
) {
    public JobPostingImportPreview {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        imageUrls = List.copyOf(imageUrls == null ? List.of() : imageUrls);
    }
}
