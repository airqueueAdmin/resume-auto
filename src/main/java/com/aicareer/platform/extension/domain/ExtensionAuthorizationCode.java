package com.aicareer.platform.extension.domain;

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
@Table(name = "extension_authorization_codes")
public class ExtensionAuthorizationCode {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 64, unique = true)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExtensionAuthorizationCode() {
    }

    private ExtensionAuthorizationCode(
            User user,
            String codeHash,
            Instant expiresAt,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    public static ExtensionAuthorizationCode create(
            User user,
            String codeHash,
            Instant expiresAt,
            Instant now
    ) {
        return new ExtensionAuthorizationCode(user, codeHash, expiresAt, now);
    }

    public boolean isActive(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        if (!isActive(now)) {
            throw new IllegalStateException("Extension authorization code is no longer active");
        }
        consumedAt = now;
    }

    public User getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
