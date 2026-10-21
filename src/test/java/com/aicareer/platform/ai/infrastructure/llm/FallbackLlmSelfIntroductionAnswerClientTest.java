package com.aicareer.platform.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.aicareer.platform.ai.application.AiIntegrationException;
import com.aicareer.platform.ai.application.GroundingValidationException;
import com.aicareer.platform.ai.application.LlmSelfIntroductionAnswerClient;
import com.aicareer.platform.ai.application.LlmSelfIntroductionAnswerCompletion;
import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.RetrievedContext;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerDocument;
import com.aicareer.platform.ai.application.SelfIntroductionJobContext;
import com.aicareer.platform.retrieval.domain.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FallbackLlmSelfIntroductionAnswerClientTest {

    @Mock
    private LlmSelfIntroductionAnswerClient primary;

    @Mock
    private LlmSelfIntroductionAnswerClient fallback;

    private FallbackLlmSelfIntroductionAnswerClient client;
    private SelfIntroductionJobContext jobContext;
    private List<RetrievedContext> contexts;
    private LlmSelfIntroductionAnswerCompletion completion;

    @BeforeEach
    void setUp() {
        client = new FallbackLlmSelfIntroductionAnswerClient(primary, fallback, true);
        jobContext = new SelfIntroductionJobContext("Example Corp", "Backend Developer", "Build APIs");
        contexts = List.of(new RetrievedContext(
                "C1", UUID.randomUUID(), SourceType.EXPERIENCE, UUID.randomUUID(), 0,
                "Java API를 개발했습니다.", "Java API를 개발했습니다.", 0.9, "nomic-embed-text"
        ));
        completion = new LlmSelfIntroductionAnswerCompletion(
                new SelfIntroductionAnswerDocument(List.of(
                        new SelfIntroductionAnswerDocument.Sentence("경험을 바탕으로 기여하겠습니다.", List.of("C1"))
                )),
                new LlmUsage(10, 20, 30),
                "ollama-request"
        );
    }

    @Test
    void retriesAnIncompleteGeminiAnswerWithOllamaAndReportsFallbackMetadata() {
        when(primary.provider()).thenReturn("gemini");
        when(fallback.provider()).thenReturn("ollama");
        when(fallback.model()).thenReturn("llama3.1:8b");
        when(fallback.selfIntroductionPromptVersion()).thenReturn("self-introduction-v4");
        when(primary.generateAnswer(any(), anyString(), anyList()))
                .thenThrow(AiIntegrationException.incompleteResponse("MAX_TOKENS", null));
        when(fallback.generateAnswer(jobContext, "지원동기", contexts)).thenReturn(completion);

        LlmSelfIntroductionAnswerCompletion result = client.generateAnswer(jobContext, "지원동기", contexts);

        assertThat(result).isSameAs(completion);
        assertThat(client.provider()).isEqualTo("ollama");
        assertThat(client.model()).isEqualTo("llama3.1:8b");
        assertThat(client.selfIntroductionPromptVersion()).isEqualTo("self-introduction-v4");
        verify(fallback).generateAnswer(jobContext, "지원동기", contexts);
    }

    @Test
    void doesNotFallbackWhenGeminiRejectsTheRequest() {
        when(primary.provider()).thenReturn("gemini");
        AiIntegrationException rejected = AiIntegrationException.requestFailed("Gemini rejected request");
        when(primary.generateAnswer(any(), anyString(), anyList())).thenThrow(rejected);

        assertThatThrownBy(() -> client.generateAnswer(jobContext, "지원동기", contexts))
                .isSameAs(rejected);

        verify(fallback, never()).generateAnswer(any(), anyString(), anyList());
        assertThat(client.provider()).isEqualTo("gemini");
    }

    @Test
    void retriesAnInvalidStructuredGeminiAnswerWithOllama() {
        when(primary.provider()).thenReturn("gemini");
        when(fallback.provider()).thenReturn("ollama");
        when(primary.generateAnswer(any(), anyString(), anyList()))
                .thenThrow(AiIntegrationException.invalidResponse("Invalid JSON", null));
        when(fallback.generateAnswer(jobContext, "지원동기", contexts)).thenReturn(completion);

        LlmSelfIntroductionAnswerCompletion result = client.generateAnswer(jobContext, "지원동기", contexts);

        assertThat(result).isSameAs(completion);
        assertThat(client.provider()).isEqualTo("ollama");
        verify(fallback).generateAnswer(jobContext, "지원동기", contexts);
    }

    @Test
    void retriesAnUnavailableGeminiAnswerWithOllama() {
        when(primary.provider()).thenReturn("gemini");
        when(fallback.provider()).thenReturn("ollama");
        when(primary.generateAnswer(any(), anyString(), anyList()))
                .thenThrow(AiIntegrationException.unavailable("Gemini is unreachable"));
        when(fallback.generateAnswer(jobContext, "지원동기", contexts)).thenReturn(completion);

        LlmSelfIntroductionAnswerCompletion result = client.generateAnswer(jobContext, "지원동기", contexts);

        assertThat(result).isSameAs(completion);
        assertThat(client.provider()).isEqualTo("ollama");
        verify(fallback).generateAnswer(jobContext, "지원동기", contexts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"uncited", "unknown", "empty", "mismatched-inline"})
    void retriesParsedButInvalidGroundingWithOllama(String failure) {
        when(primary.provider()).thenReturn("gemini");
        when(fallback.provider()).thenReturn("ollama");
        var invalid = switch (failure) {
            case "unknown" -> answer("Java API를 개발했습니다.", List.of("C99"));
            case "empty" -> answer("...", List.of("C1"));
            case "mismatched-inline" -> answer("Java API를 개발했습니다 [C99].", List.of("C1"));
            default -> answer("Java API를 개발했습니다.", List.of());
        };
        when(primary.generateAnswer(jobContext, "지원동기", contexts)).thenReturn(invalid);
        when(fallback.generateAnswer(jobContext, "지원동기", contexts)).thenReturn(completion);

        assertThat(client.generateAnswer(jobContext, "지원동기", contexts)).isSameAs(completion);
        assertThat(client.provider()).isEqualTo("ollama");
        verify(fallback).generateAnswer(jobContext, "지원동기", contexts);
    }

    @Test
    void rejectsInvalidFallbackCitationsInsteadOfReturningUnsupportedText() {
        when(primary.provider()).thenReturn("gemini");
        when(fallback.provider()).thenReturn("ollama");
        when(primary.generateAnswer(jobContext, "지원동기", contexts))
                .thenThrow(AiIntegrationException.incompleteResponse("MAX_TOKENS", null));
        when(fallback.generateAnswer(jobContext, "지원동기", contexts))
                .thenReturn(answer("Java API를 개발했습니다.", List.of("C99")));

        assertThatThrownBy(() -> client.generateAnswer(jobContext, "지원동기", contexts))
                .isInstanceOf(GroundingValidationException.class);
    }

    @Test
    void doesNotTurnAnExplicitAbstentionIntoAFabricatedFallbackAnswer() {
        when(primary.generateAnswer(jobContext, "지원동기", contexts)).thenReturn(
                new LlmSelfIntroductionAnswerCompletion(new SelfIntroductionAnswerDocument(List.of()),
                        new LlmUsage(1, 2, 3), "gemini-answer"));

        assertThatThrownBy(() -> client.generateAnswer(jobContext, "지원동기", contexts))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(error -> ((GroundingValidationException) error).kind())
                .isEqualTo(GroundingValidationException.Kind.ANSWER_UNSUPPORTED);
        verify(fallback, never()).generateAnswer(any(), anyString(), anyList());
    }

    private LlmSelfIntroductionAnswerCompletion answer(String text, List<String> citationIds) {
        return new LlmSelfIntroductionAnswerCompletion(
                new SelfIntroductionAnswerDocument(List.of(
                        new SelfIntroductionAnswerDocument.Sentence(text, citationIds)
                )), new LlmUsage(1, 2, 3), "gemini-answer"
        );
    }
}
