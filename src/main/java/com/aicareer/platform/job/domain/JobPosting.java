package com.aicareer.platform.job.domain;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_postings")
public class JobPosting {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected JobPosting() {
    }

    private JobPosting(
            User user,
            String title,
            String companyName,
            String description,
            String sourceUrl,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.title = title;
        this.companyName = companyName;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static JobPosting create(
            User user,
            String title,
            String companyName,
            String description,
            String sourceUrl,
            Instant now
    ) {
        return new JobPosting(user, title, companyName, description, sourceUrl, now);
    }

    public void update(
            String title,
            String companyName,
            String description,
            String sourceUrl,
            Instant now
    ) {
        this.title = title;
        this.companyName = companyName;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
