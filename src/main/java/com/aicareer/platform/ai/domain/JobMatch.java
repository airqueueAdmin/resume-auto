package com.aicareer.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.job.domain.JobPosting;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_job_matches")
public class JobMatch {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 100)
    private String promptVersion;

    @Column(name = "provider_response_id", nullable = false, length = 200)
    private String providerResponseId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode analysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_references", nullable = false, columnDefinition = "jsonb")
    private JsonNode sourceReferences;

    @Column(name = "retrieved_chunk_count", nullable = false)
    private int retrievedChunkCount;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobMatch() {
    }

    private JobMatch(
            User user,
            JobPosting jobPosting,
            String provider,
            String model,
            String promptVersion,
            String providerResponseId,
            JsonNode analysis,
            JsonNode sourceReferences,
            int retrievedChunkCount,
            LlmUsage usage,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.jobPosting = jobPosting;
        this.provider = provider;
        this.model = model;
        this.promptVersion = promptVersion;
        this.providerResponseId = providerResponseId;
        this.analysis = analysis;
        this.sourceReferences = sourceReferences;
        this.retrievedChunkCount = retrievedChunkCount;
        this.inputTokens = usage.inputTokens();
        this.outputTokens = usage.outputTokens();
        this.totalTokens = usage.totalTokens();
        this.createdAt = createdAt;
    }

    public static JobMatch create(
            User user,
            JobPosting jobPosting,
            String provider,
            String model,
            String promptVersion,
            String providerResponseId,
            JsonNode analysis,
            JsonNode sourceReferences,
            int retrievedChunkCount,
            LlmUsage usage,
            Instant createdAt
    ) {
        return new JobMatch(
                user,
                jobPosting,
                provider,
                model,
                promptVersion,
                providerResponseId,
                analysis,
                sourceReferences,
                retrievedChunkCount,
                usage,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public int getRetrievedChunkCount() {
        return retrievedChunkCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
