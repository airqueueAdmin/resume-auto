package com.aicareer.platform.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EvaluationDatasetContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void versionedFixtureContainsOneCaseForEachInitialEvaluationTask() throws IOException {
        List<EvaluationCase> cases;
        try (InputStream input = getClass().getResourceAsStream(
                "/evaluation/phase-8/cases.json")) {
            assertThat(input).isNotNull();
            cases = objectMapper.readValue(input, new TypeReference<>() {
            });
        }

        assertThat(cases).hasSize(3);
        assertThat(cases).extracting(EvaluationCase::caseId)
                .doesNotHaveDuplicates();
        assertThat(cases).extracting(EvaluationCase::task)
                .containsExactlyInAnyOrder(EvaluationTask.values());
        assertThat(cases).allSatisfy(evaluationCase -> {
            assertThat(evaluationCase.topK()).isBetween(1, 20);
            assertThat(evaluationCase.expectedSourceIds()).isNotEmpty();
        });
    }
}
