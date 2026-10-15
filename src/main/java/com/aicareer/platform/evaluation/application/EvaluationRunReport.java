package com.aicareer.platform.evaluation.application;

import java.util.List;

public record EvaluationRunReport(
        String datasetVersion,
        List<EvaluationCaseResult> cases,
        EvaluationReport summary
) {
    public EvaluationRunReport {
        if (datasetVersion == null || datasetVersion.isBlank()) {
            throw new IllegalArgumentException("Dataset version must not be blank");
        }
        if (cases == null || cases.isEmpty() || summary == null) {
            throw new IllegalArgumentException("Evaluation run must contain cases and a summary");
        }
        cases = List.copyOf(cases);
    }
}
