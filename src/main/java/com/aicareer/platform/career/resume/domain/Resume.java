package com.aicareer.platform.career.resume.domain;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.shared.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "latest_version_number", nullable = false)
    private int latestVersionNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Resume() {
    }

    private Resume(User user, String name, Instant now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = name;
        this.latestVersionNumber = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Resume create(User user, String name, Instant now) {
        return new Resume(user, name, now);
    }

    public void rename(String name, Instant now) {
        this.name = name;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public int nextVersionNumber() {
        return latestVersionNumber + 1;
    }

    public void markVersionCreated(int versionNumber, Instant now) {
        if (versionNumber != nextVersionNumber()) {
            throw new IllegalArgumentException("versionNumber must be the next sequential version");
        }
        this.latestVersionNumber = versionNumber;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLatestVersionNumber() {
        return latestVersionNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
