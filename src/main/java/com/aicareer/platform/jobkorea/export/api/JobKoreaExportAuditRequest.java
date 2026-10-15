package com.aicareer.platform.jobkorea.export.api;

import java.util.UUID;

import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportResult;
import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportTemplate;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record JobKoreaExportAuditRequest(
        @NotNull UUID resumeVersionId,
        @NotNull JobKoreaExportTemplate templateVersion,
        @NotNull JobKoreaExportResult result,
        @Positive @Max(5 * 1024 * 1024) Long fileSizeBytes
) {

    @AssertTrue(message = "fileSizeBytes is required only for a triggered download")
    public boolean isFileSizeConsistent() {
        if (result == null) {
            return true;
        }
        return result == JobKoreaExportResult.DOWNLOAD_TRIGGERED
                ? fileSizeBytes != null
                : fileSizeBytes == null;
    }
}
