package com.aicareer.platform.evaluation.application;

public record EvaluationCostResult(
        String caseId,
        EvaluationExecutionMetadata metadata,
        EvaluationEstimatedCost cost
) {
    public EvaluationCostResult {
        if (caseId == null || caseId.isBlank() || metadata == null || cost == null) {
            throw new IllegalArgumentException("Evaluation cost result is incomplete");
        }
    }
}
