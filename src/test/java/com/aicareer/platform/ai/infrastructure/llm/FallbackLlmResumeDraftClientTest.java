package com.aicareer.platform.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.aicareer.platform.ai.application.AiIntegrationException;
import com.aicareer.platform.ai.application.LlmResumeDraftClient;
import com.aicareer.platform.ai.application.LlmResumeDraftCompletion;
import com.aicareer.platform.job.domain.JobPosting;
import org.junit.jupiter.api.Test;

class FallbackLlmResumeDraftClientTest {

    @Test
    void usesFallbackWhenPrimaryResponseIsIncomplete() {
        LlmResumeDraftClient primary = mock(LlmResumeDraftClient.class);
        LlmResumeDraftClient fallback = mock(LlmResumeDraftClient.class);
        JobPosting jobPosting = mock(JobPosting.class);
        LlmResumeDraftCompletion completion = mock(LlmResumeDraftCompletion.class);
        when(primary.provider()).thenReturn("gemini");
        when(fallback.provider()).thenReturn("ollama");
        when(primary.generateResumeDraft(any(), any()))
                .thenThrow(AiIntegrationException.incompleteResponse("MAX_TOKENS", null));
        when(fallback.generateResumeDraft(jobPosting, List.of())).thenReturn(completion);

        FallbackLlmResumeDraftClient client = new FallbackLlmResumeDraftClient(
                primary, fallback, true
        );

        assertThat(client.generateResumeDraft(jobPosting, List.of())).isSameAs(completion);
        assertThat(client.provider()).isEqualTo("ollama");
        verify(fallback).generateResumeDraft(jobPosting, List.of());
    }
}
