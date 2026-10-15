package com.aicareer.platform.evaluation.application;

public record EvaluationCaseResult(
        String caseId,
        EvaluationTask task,
        EvaluationExecutionMetadata metadata,
        EvaluationMetrics metrics
) {
    public EvaluationCaseResult {
        if (caseId == null || caseId.isBlank() || task == null || metadata == null || metrics == null) {
            throw new IllegalArgumentException("Evaluation case result is incomplete");
        }
    }
}
