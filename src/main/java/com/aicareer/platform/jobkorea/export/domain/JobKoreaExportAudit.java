package com.aicareer.platform.jobkorea.export.domain;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.ResumeVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "jobkorea_export_audits")
public class JobKoreaExportAudit {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_version_id", nullable = false, updatable = false)
    private ResumeVersion resumeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_version", nullable = false, length = 50, updatable = false)
    private JobKoreaExportTemplate templateVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private JobKoreaExportResult result;

    @Column(name = "file_size_bytes", updatable = false)
    private Long fileSizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JobKoreaExportAudit() {
    }

    private JobKoreaExportAudit(
            UUID userId,
            ResumeVersion resumeVersion,
            JobKoreaExportTemplate templateVersion,
            JobKoreaExportResult result,
            Long fileSizeBytes,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.resumeVersion = resumeVersion;
        this.templateVersion = templateVersion;
        this.result = result;
        this.fileSizeBytes = fileSizeBytes;
        this.createdAt = createdAt;
    }

    public static JobKoreaExportAudit create(
            UUID userId,
            ResumeVersion resumeVersion,
            JobKoreaExportTemplate templateVersion,
            JobKoreaExportResult result,
            Long fileSizeBytes,
            Instant createdAt
    ) {
        return new JobKoreaExportAudit(
                userId,
                resumeVersion,
                templateVersion,
                result,
                fileSizeBytes,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getResumeVersionId() {
        return resumeVersion.getId();
    }

    public JobKoreaExportTemplate getTemplateVersion() {
        return templateVersion;
    }

    public JobKoreaExportResult getResult() {
        return result;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
