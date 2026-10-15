package com.aicareer.platform.evaluation.application;

public record EvaluationCaseExecution(
        EvaluationExecutionMetadata metadata,
        EvaluationObservation observation
) {
    public EvaluationCaseExecution {
        if (metadata == null || observation == null) {
            throw new IllegalArgumentException("Evaluation execution metadata and observation are required");
        }
    }
}
