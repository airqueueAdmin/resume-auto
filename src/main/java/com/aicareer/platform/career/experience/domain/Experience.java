package com.aicareer.platform.career.experience.domain;

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
@Table(name = "experiences")
public class Experience {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String role;

    @Column(length = 200)
    private String department;

    @Column(length = 200)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "currently_employed", nullable = false)
    private boolean currentlyEmployed;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "tech_stack", length = 1000)
    private String techStack;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Experience() {
    }

    private Experience(
            User user,
            String companyName,
            String role,
            String department,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            boolean currentlyEmployed,
            String description,
            String techStack,
            Instant now
    ) {
        validatePeriod(startDate, endDate, currentlyEmployed);
        this.id = UUID.randomUUID();
        this.user = user;
        this.companyName = companyName;
        this.role = role;
        this.department = department;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyEmployed = currentlyEmployed;
        this.description = description;
        this.techStack = techStack;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Experience create(
            User user,
            String companyName,
            String role,
            String department,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            boolean currentlyEmployed,
            String description,
            String techStack,
            Instant now
    ) {
        return new Experience(user, companyName, role, department, title, startDate, endDate, currentlyEmployed, description, techStack, now);
    }

    public static Experience create(
            User user,
            String companyName,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            boolean currentlyEmployed,
            String description,
            String techStack,
            Instant now
    ) {
        return new Experience(user, companyName, role, null, null, startDate, endDate, currentlyEmployed, description, techStack, now);
    }

    public void update(
            String companyName,
            String role,
            String department,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            boolean currentlyEmployed,
            String description,
            String techStack,
            Instant now
    ) {
        validatePeriod(startDate, endDate, currentlyEmployed);
        this.companyName = companyName;
        this.role = role;
        this.department = department;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyEmployed = currentlyEmployed;
        this.description = description;
        this.techStack = techStack;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate, boolean currentlyEmployed) {
        if (currentlyEmployed && endDate != null) {
            throw new IllegalArgumentException("endDate must be empty when currentlyEmployed is true");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRole() {
        return role;
    }

    public String getDepartment() {
        return department;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isCurrentlyEmployed() {
        return currentlyEmployed;
    }

    public String getDescription() {
        return description;
    }

    public String getTechStack() {
        return techStack;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
