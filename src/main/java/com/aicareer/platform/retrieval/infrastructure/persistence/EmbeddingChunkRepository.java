package com.aicareer.platform.retrieval.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.domain.SourceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmbeddingChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmbeddingChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteAllForUser(UUID userId) {
        jdbcTemplate.update(
                "DELETE FROM embedding_chunks WHERE user_id = ?",
                userId
        );
    }

    public void insert(
            UUID chunkId,
            UUID userId,
            UUID experienceId,
            UUID projectId,
            int chunkIndex,
            String content,
            String contentHash,
            double[] embedding,
            String embeddingModel,
            int embeddingDimensions,
            Timestamp createdAt,
            Timestamp updatedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO embedding_chunks (
                    id, user_id, experience_id, project_id, chunk_index, content, content_hash,
                    embedding, embedding_model, embedding_dimensions, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?)
                """,
                chunkId,
                userId,
                experienceId,
                projectId,
                chunkIndex,
                content,
                contentHash,
                vectorLiteral(embedding),
                embeddingModel,
                embeddingDimensions,
                createdAt,
                updatedAt
        );
    }

    public List<EmbeddingChunkSearchResult> search(
            UUID userId,
            double[] queryEmbedding,
            int topK,
            Set<SourceType> sourceTypes
    ) {
        String sourceFilter = sourceFilter(sourceTypes);
        String sql = """
                WITH query_embedding AS (
                    SELECT ?::vector AS embedding
                )
                SELECT c.id,
                       CASE WHEN c.experience_id IS NOT NULL THEN 'EXPERIENCE' ELSE 'PROJECT' END AS source_type,
                       CASE WHEN c.experience_id IS NOT NULL THEN c.experience_id ELSE c.project_id END AS source_id,
                       c.chunk_index,
                       c.content,
                       1 - (c.embedding <=> q.embedding) AS similarity,
                       c.embedding_model
                FROM embedding_chunks c
                CROSS JOIN query_embedding q
                LEFT JOIN experiences e ON e.id = c.experience_id
                LEFT JOIN projects p ON p.id = c.project_id
                WHERE c.user_id = ?
                  AND ((c.experience_id IS NOT NULL AND e.deleted_at IS NULL)
                    OR (c.project_id IS NOT NULL AND p.deleted_at IS NULL))
                  AND %s
                ORDER BY c.embedding <=> q.embedding ASC, c.id ASC
                LIMIT ?
                """.formatted(sourceFilter);

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new EmbeddingChunkSearchResult(
                        resultSet.getObject("id", UUID.class),
                        SourceType.valueOf(resultSet.getString("source_type")),
                        resultSet.getObject("source_id", UUID.class),
                        resultSet.getInt("chunk_index"),
                        resultSet.getString("content"),
                        resultSet.getDouble("similarity"),
                        resultSet.getString("embedding_model")
                ),
                vectorLiteral(queryEmbedding),
                userId,
                topK
        );
    }

    public boolean existsActiveForUser(UUID userId, Set<SourceType> sourceTypes) {
        String sourceFilter = sourceFilter(sourceTypes);
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM embedding_chunks c
                    LEFT JOIN experiences e ON e.id = c.experience_id
                    LEFT JOIN projects p ON p.id = c.project_id
                    WHERE c.user_id = ?
                      AND ((c.experience_id IS NOT NULL AND e.deleted_at IS NULL)
                        OR (c.project_id IS NOT NULL AND p.deleted_at IS NULL))
                      AND %s
                )
                """.formatted(sourceFilter);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
    }

    public long countActiveForUser(UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM embedding_chunks c
                LEFT JOIN experiences e ON e.id = c.experience_id
                LEFT JOIN projects p ON p.id = c.project_id
                WHERE c.user_id = ?
                  AND ((c.experience_id IS NOT NULL AND e.deleted_at IS NULL)
                    OR (c.project_id IS NOT NULL AND p.deleted_at IS NULL))
                """,
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public List<EmbeddingChunkSearchResult> findAllActiveForUser(UUID userId) {
        String sql = """
                SELECT c.id,
                       CASE WHEN c.experience_id IS NOT NULL THEN 'EXPERIENCE' ELSE 'PROJECT' END AS source_type,
                       CASE WHEN c.experience_id IS NOT NULL THEN c.experience_id ELSE c.project_id END AS source_id,
                       c.chunk_index,
                       c.content,
                       c.embedding_model
                FROM embedding_chunks c
                LEFT JOIN experiences e ON e.id = c.experience_id
                LEFT JOIN projects p ON p.id = c.project_id
                WHERE c.user_id = ?
                  AND ((c.experience_id IS NOT NULL AND e.deleted_at IS NULL)
                    OR (c.project_id IS NOT NULL AND p.deleted_at IS NULL))
                ORDER BY source_type, source_id, c.chunk_index, c.id
                """;

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new EmbeddingChunkSearchResult(
                        resultSet.getObject("id", UUID.class),
                        SourceType.valueOf(resultSet.getString("source_type")),
                        resultSet.getObject("source_id", UUID.class),
                        resultSet.getInt("chunk_index"),
                        resultSet.getString("content"),
                        0.0,
                        resultSet.getString("embedding_model")
                ),
                userId
        );
    }

    private String sourceFilter(Set<SourceType> sourceTypes) {
        if (sourceTypes == null || sourceTypes.isEmpty() || sourceTypes.size() == 2) {
            return "TRUE";
        }
        return sourceTypes.stream()
                .map(sourceType -> switch (sourceType) {
                    case EXPERIENCE -> "c.experience_id IS NOT NULL";
                    case PROJECT -> "c.project_id IS NOT NULL";
                })
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    private String vectorLiteral(double[] embedding) {
        String values = java.util.Arrays.stream(embedding)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(","));
        return "[" + values + "]";
    }
}
