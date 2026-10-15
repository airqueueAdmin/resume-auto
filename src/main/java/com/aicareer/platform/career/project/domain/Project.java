package com.aicareer.platform.career.project.domain;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "projects")
public class Project {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String role;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "tech_stack", nullable = false, length = 1000)
    private String techStack;

    @Column(name = "repository_url", length = 2048)
    private String repositoryUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Project() {
    }

    private Project(
            User user,
            String name,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            String techStack,
            String repositoryUrl,
            Instant now
    ) {
        validateDateRange(startDate, endDate);
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = name;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.techStack = techStack;
        this.repositoryUrl = repositoryUrl;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Project create(
            User user,
            String name,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            String techStack,
            String repositoryUrl,
            Instant now
    ) {
        return new Project(
                user,
                name,
                role,
                startDate,
                endDate,
                description,
                techStack,
                repositoryUrl,
                now
        );
    }

    public void update(
            String name,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            String techStack,
            String repositoryUrl,
            Instant now
    ) {
        validateDateRange(startDate, endDate);
        this.name = name;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.techStack = techStack;
        this.repositoryUrl = repositoryUrl;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public String getTechStack() {
        return techStack;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
