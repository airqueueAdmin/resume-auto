package com.aicareer.platform.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailVerificationCode() {
    }

    private EmailVerificationCode(
            User user,
            String codeHash,
            Instant expiresAt,
            int maxAttempts,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
        this.createdAt = createdAt;
    }

    public static EmailVerificationCode issue(
            User user,
            String codeHash,
            Instant expiresAt,
            int maxAttempts,
            Instant createdAt
    ) {
        return new EmailVerificationCode(user, codeHash, expiresAt, maxAttempts, createdAt);
    }

    public boolean canAttempt(Instant now) {
        return consumedAt == null && now.isBefore(expiresAt) && failedAttempts < maxAttempts;
    }

    public boolean canResendAfter(Instant now, java.time.Duration cooldown) {
        return !now.isBefore(createdAt.plus(cooldown));
    }

    public void recordFailedAttempt(Instant now) {
        if (consumedAt != null) {
            return;
        }
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            consumedAt = now;
        }
    }

    public void consume(Instant now) {
        if (consumedAt == null) {
            consumedAt = now;
        }
    }

    public String getCodeHash() {
        return codeHash;
    }
}
