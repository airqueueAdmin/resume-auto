package com.aicareer.platform.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.aicareer.platform.ai.application.LlmUsage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EvaluationAnnotationContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsVersionedHumanAnnotationFixtureAndAppliesReviewedClaimSupport() throws IOException {
        List<EvaluationAnnotation> annotations;
        try (InputStream input = getClass().getResourceAsStream(
                "/evaluation/phase-8/annotations.json")) {
            assertThat(input).isNotNull();
            annotations = objectMapper.readValue(input, new TypeReference<>() {
            });
        }

        assertThat(annotations).hasSize(3);
        assertThat(annotations).extracting(EvaluationAnnotation::caseId)
                .containsExactlyInAnyOrder(
                        "backend-api-retrieval", "grounded-job-match", "grounded-resume-draft"
                );

        EvaluationObservation observation = new EvaluationObservation(
                Set.of("source-a"),
                List.of("source-a"),
                List.of("source-a"),
                2,
                0,
                20,
                new LlmUsage(10, 5, 15)
        );
        EvaluationAnnotation annotation = annotations.stream()
                .filter(item -> item.caseId().equals("grounded-job-match"))
                .findFirst()
                .orElseThrow();

        EvaluationObservation reviewed = annotation.applyTo(observation);

        assertThat(reviewed.claimCount()).isEqualTo(2);
        assertThat(reviewed.supportedClaimCount()).isEqualTo(2);
    }

    @Test
    void rejectsAnnotationWhenClaimCountDoesNotMatchObservation() {
        EvaluationAnnotation annotation = new EvaluationAnnotation(
                "case", "grounding-v1", "reviewer", "2026-10-13", 2, 1, ""
        );
        EvaluationObservation observation = new EvaluationObservation(
                Set.of("source"), List.of("source"), List.of("source"), 1, 1, 1,
                new LlmUsage(1, 1, 2)
        );

        assertThatThrownBy(() -> annotation.applyTo(observation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("claim count");
    }
}
