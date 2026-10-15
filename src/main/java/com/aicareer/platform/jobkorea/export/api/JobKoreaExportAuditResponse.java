package com.aicareer.platform.jobkorea.export.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportAudit;
import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportResult;
import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportTemplate;

public record JobKoreaExportAuditResponse(
        UUID id,
        UUID resumeVersionId,
        JobKoreaExportTemplate templateVersion,
        JobKoreaExportResult result,
        Long fileSizeBytes,
        Instant createdAt
) {

    public static JobKoreaExportAuditResponse from(JobKoreaExportAudit audit) {
        return new JobKoreaExportAuditResponse(
                audit.getId(),
                audit.getResumeVersionId(),
                audit.getTemplateVersion(),
                audit.getResult(),
                audit.getFileSizeBytes(),
                audit.getCreatedAt()
        );
    }
}
