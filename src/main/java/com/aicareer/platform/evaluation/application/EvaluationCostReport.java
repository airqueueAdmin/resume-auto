package com.aicareer.platform.evaluation.application;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationCostReport(
        String datasetVersion,
        String currency,
        List<EvaluationCostResult> cases,
        BigDecimal totalCost
) {
    public EvaluationCostReport {
        if (datasetVersion == null || datasetVersion.isBlank()
                || currency == null || cases == null || cases.isEmpty() || totalCost == null
                || totalCost.signum() < 0) {
            throw new IllegalArgumentException("Evaluation cost report is incomplete");
        }
        cases = List.copyOf(cases);
    }
}
