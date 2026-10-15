package com.aicareer.platform.tool.domain;

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
@Table(name = "tool_approvals")
public class ToolApproval {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "tool_name", nullable = false, length = 100, updatable = false)
    private String toolName;

    @Column(name = "arguments_hash", nullable = false, length = 64, updatable = false)
    private String argumentsHash;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ToolApproval() {
    }

    private ToolApproval(
            User user,
            String toolName,
            String argumentsHash,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.toolName = toolName;
        this.argumentsHash = argumentsHash;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static ToolApproval create(
            User user,
            String toolName,
            String argumentsHash,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        return new ToolApproval(user, toolName, argumentsHash, tokenHash, expiresAt, createdAt);
    }

    public boolean belongsTo(UUID userId) {
        return user.getId().equals(userId);
    }

    public boolean matches(String toolName, String argumentsHash, Instant now) {
        return this.toolName.equals(toolName)
                && this.argumentsHash.equals(argumentsHash)
                && usedAt == null
                && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        if (usedAt != null) {
            throw new IllegalStateException("Tool approval is already used");
        }
        usedAt = now;
    }
}
