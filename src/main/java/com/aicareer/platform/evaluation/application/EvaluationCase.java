package com.aicareer.platform.evaluation.application;

import java.util.List;

public record EvaluationCase(
        String caseId,
        EvaluationTask task,
        String query,
        int topK,
        List<String> expectedSourceIds,
        String notes
) {
    public EvaluationCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("Evaluation case ID must not be blank");
        }
        if (task == null) {
            throw new IllegalArgumentException("Evaluation task must not be null");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Evaluation query must not be blank");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("Evaluation topK must be positive");
        }
        expectedSourceIds = normalizedIds(expectedSourceIds, "expectedSourceIds");
        notes = notes == null ? "" : notes;
    }

    private static List<String> normalizedIds(List<String> ids, String fieldName) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        List<String> normalized = ids.stream()
                .map(id -> id == null ? "" : id.trim())
                .toList();
        if (normalized.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(fieldName + " must not contain blank identifiers");
        }
        if (normalized.size() != normalized.stream().distinct().count()) {
            throw new IllegalArgumentException(fieldName + " must not contain duplicate identifiers");
        }
        return List.copyOf(normalized);
    }
}
