package com.aicareer.platform.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import com.aicareer.platform.ai.application.LlmUsage;
import org.junit.jupiter.api.Test;

class EvaluationRunnerTest {

    @Test
    void runsDeterministicStubExecutionsAndRecordsPerCaseMetadata() {
        List<EvaluationCase> cases = List.of(
                new EvaluationCase(
                        "retrieval-case",
                        EvaluationTask.RETRIEVAL,
                        "backend APIs",
                        3,
                        List.of("experience-a"),
                        "retrieval"
                ),
                new EvaluationCase(
                        "matching-case",
                        EvaluationTask.MATCHING,
                        "Spring services",
                        3,
                        List.of("experience-a", "project-b"),
                        "matching"
                ),
                new EvaluationCase(
                        "resume-case",
                        EvaluationTask.RESUME_DRAFT,
                        "platform resume",
                        3,
                        List.of("project-b"),
                        "resume"
                )
        );
        EvaluationRunner runner = new EvaluationRunner(new EvaluationMetricCalculator());

        EvaluationRunReport report = runner.run(
                "phase-8-v1",
                cases,
                evaluationCase -> switch (evaluationCase.task()) {
                    case RETRIEVAL -> execution("embedding-stub", "embedding-test", "retrieval-v1",
                            Set.of("experience-a"), List.of("experience-a", "noise"), List.of(),
                            0, 0, 20, new LlmUsage(0, 0, 0));
                    case MATCHING -> execution("match-stub", "match-test", "job-match-v1",
                            Set.of("experience-a", "project-b"), List.of("experience-a", "project-b"),
                            List.of("experience-a", "project-b"), 2, 2, 120, new LlmUsage(80, 30, 110));
                    case RESUME_DRAFT -> execution("resume-stub", "resume-test", "resume-draft-v1",
                            Set.of("project-b"), List.of("project-b"), List.of("project-b"),
                            3, 3, 180, new LlmUsage(90, 40, 130));
                }
        );

        assertThat(report.datasetVersion()).isEqualTo("phase-8-v1");
        assertThat(report.cases()).hasSize(3);
        assertThat(report.cases()).extracting(EvaluationCaseResult::caseId)
                .containsExactly("retrieval-case", "matching-case", "resume-case");
        assertThat(report.cases().get(1).metadata().promptVersion()).isEqualTo("job-match-v1");
        assertThat(report.cases().get(2).metrics().unsupportedClaimRate()).isZero();
        assertThat(report.summary().totalTokens()).isEqualTo(240);
        assertThat(report.summary().meanLatencyMillis()).isEqualTo(320.0 / 3.0);
    }

    @Test
    void rejectsDuplicateCasesAndMissingStubExecution() {
        EvaluationCase evaluationCase = new EvaluationCase(
                "duplicate",
                EvaluationTask.RETRIEVAL,
                "query",
                1,
                List.of("source"),
                ""
        );
        EvaluationRunner runner = new EvaluationRunner(new EvaluationMetricCalculator());

        assertThatThrownBy(() -> runner.run(
                "phase-8-v1",
                List.of(evaluationCase, evaluationCase),
                ignored -> execution("stub", "model", "prompt-v1", Set.of("source"), List.of("source"),
                        List.of(), 0, 0, 1, new LlmUsage(0, 0, 0))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");

        assertThatThrownBy(() -> runner.run("phase-8-v1", List.of(evaluationCase), ignored -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    private EvaluationCaseExecution execution(
            String provider,
            String model,
            String promptVersion,
            Set<String> expected,
            List<String> retrieved,
            List<String> cited,
            int claimCount,
            int supportedClaimCount,
            long latencyMillis,
            LlmUsage usage
    ) {
        return new EvaluationCaseExecution(
                new EvaluationExecutionMetadata(provider, model, promptVersion),
                new EvaluationObservation(
                        expected,
                        retrieved,
                        cited,
                        claimCount,
                        supportedClaimCount,
                        latencyMillis,
                        usage
                )
        );
    }
}
