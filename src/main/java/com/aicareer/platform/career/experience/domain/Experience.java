package com.aicareer.platform.career.experience.domain;

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

    @Column(nullable = false, columnDefinition = "text")
    private String description;

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
            String description,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.companyName = companyName;
        this.role = role;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Experience create(
            User user,
            String companyName,
            String role,
            String description,
            Instant now
    ) {
        return new Experience(user, companyName, role, description, now);
    }

    public void update(
            String companyName,
            String role,
            String description,
            Instant now
    ) {
        this.companyName = companyName;
        this.role = role;
        this.description = description;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
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

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
