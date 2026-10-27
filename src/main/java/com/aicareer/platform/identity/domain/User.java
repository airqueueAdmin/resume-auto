package com.aicareer.platform.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    private User(String email, String passwordHash, String displayName, UserStatus status, Instant now) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
        this.emailVerifiedAt = status == UserStatus.ACTIVE ? now : null;
    }

    public static User create(String email, String passwordHash, String displayName, Instant now) {
        return new User(email, passwordHash, displayName, UserStatus.ACTIVE, now);
    }

    public static User createPendingEmailVerification(
            String email,
            String passwordHash,
            String displayName,
            Instant now
    ) {
        return new User(email, passwordHash, displayName, UserStatus.PENDING_EMAIL_VERIFICATION, now);
    }

    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public boolean isEmailVerificationPending() {
        return status == UserStatus.PENDING_EMAIL_VERIFICATION && deletedAt == null;
    }

    public void verifyEmail(Instant now) {
        if (!isEmailVerificationPending()) {
            throw new IllegalStateException("Only a pending account can verify its email");
        }
        status = UserStatus.ACTIVE;
        emailVerifiedAt = now;
        updatedAt = now;
    }

    public void delete(Instant now) {
        if (deletedAt != null) {
            return;
        }
        status = UserStatus.DELETED;
        deletedAt = now;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }
}
