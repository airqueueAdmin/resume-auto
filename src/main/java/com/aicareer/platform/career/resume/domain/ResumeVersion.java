package com.aicareer.platform.career.resume.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import com.aicareer.platform.shared.security.EncryptedStringConverter;

@Entity
@Immutable
@Table(name = "resume_versions")
public class ResumeVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, updatable = false)
    private Resume resume;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ResumeVersion() {
    }

    private ResumeVersion(Resume resume, int versionNumber, String content, Instant now) {
        this.id = UUID.randomUUID();
        this.resume = resume;
        this.versionNumber = versionNumber;
        this.content = content;
        this.createdAt = now;
    }

    public static ResumeVersion create(
            Resume resume,
            int versionNumber,
            String content,
            Instant now
    ) {
        return new ResumeVersion(resume, versionNumber, content, now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getResumeId() {
        return resume.getId();
    }

    public String getResumeName() {
        return resume.getName();
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
