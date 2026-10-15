package com.aicareer.platform.jobkorea.export.application;

import java.util.UUID;

import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportResult;
import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportTemplate;

public record RecordJobKoreaExportAuditCommand(
        UUID resumeVersionId,
        JobKoreaExportTemplate templateVersion,
        JobKoreaExportResult result,
        Long fileSizeBytes
) {
}
