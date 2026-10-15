package com.aicareer.platform.evaluation.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class EvaluationRunner {

    private final EvaluationMetricCalculator metricCalculator;

    public EvaluationRunner(EvaluationMetricCalculator metricCalculator) {
        if (metricCalculator == null) {
            throw new IllegalArgumentException("Metric calculator must not be null");
        }
        this.metricCalculator = metricCalculator;
    }

    public EvaluationRunReport run(
            String datasetVersion,
            List<EvaluationCase> cases,
            Function<EvaluationCase, EvaluationCaseExecution> executionSource
    ) {
        if (datasetVersion == null || datasetVersion.isBlank()) {
            throw new IllegalArgumentException("Dataset version must not be blank");
        }
        if (cases == null || cases.isEmpty() || cases.stream().anyMatch(evaluationCase -> evaluationCase == null)) {
            throw new IllegalArgumentException("Evaluation cases must contain at least one non-null case");
        }
        if (executionSource == null) {
            throw new IllegalArgumentException("Evaluation execution source must not be null");
        }
        Set<String> caseIds = new HashSet<>();
        List<EvaluationCaseResult> results = cases.stream()
                .map(evaluationCase -> resultFor(evaluationCase, executionSource, caseIds))
                .toList();
        EvaluationReport summary = metricCalculator.summarize(
                results.stream().map(EvaluationCaseResult::metrics).toList()
        );
        return new EvaluationRunReport(datasetVersion, results, summary);
    }

    private EvaluationCaseResult resultFor(
            EvaluationCase evaluationCase,
            Function<EvaluationCase, EvaluationCaseExecution> executionSource,
            Set<String> caseIds
    ) {
        if (!caseIds.add(evaluationCase.caseId())) {
            throw new IllegalArgumentException("Evaluation case IDs must be unique");
        }
        EvaluationCaseExecution execution = executionSource.apply(evaluationCase);
        if (execution == null) {
            throw new IllegalArgumentException("Evaluation execution source returned null");
        }
        return new EvaluationCaseResult(
                evaluationCase.caseId(),
                evaluationCase.task(),
                execution.metadata(),
                metricCalculator.calculate(execution.observation())
        );
    }
}
