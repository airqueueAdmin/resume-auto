package com.aicareer.platform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.aicareer.platform.ai.infrastructure.config.SelfIntroductionProperties;
import com.aicareer.platform.career.resume.question.application.ResumeApplicationQuestionService;
import com.aicareer.platform.career.resume.question.domain.ResumeApplicationQuestion;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.application.VectorSearchOutcome;
import com.aicareer.platform.retrieval.application.VectorSearchService;
import com.aicareer.platform.retrieval.domain.SourceType;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class SelfIntroductionAnswerServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESUME_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID JOB_POSTING_ID = UUID.randomUUID();

    @Test
    void returnsEditableAnswerWithResolvedCareerCitations() {
        ResumeApplicationQuestionService questionService = mock(ResumeApplicationQuestionService.class);
        JobPostingService jobPostingService = mock(JobPostingService.class);
        VectorSearchService searchService = mock(VectorSearchService.class);
        LlmSelfIntroductionAnswerClient llmClient = mock(LlmSelfIntroductionAnswerClient.class);
        ResumeApplicationQuestion applicationQuestion = applicationQuestion("지원동기를 작성해주세요.");
        when(questionService.get(USER_ID, RESUME_ID, QUESTION_ID)).thenReturn(applicationQuestion);
        when(jobPostingService.get(USER_ID, JOB_POSTING_ID)).thenReturn(jobPosting());
        EmbeddingChunkSearchResult evidence = evidence();
        when(searchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(evidence), "nomic-embed-text", 768));
        when(llmClient.generateAnswer(any(SelfIntroductionJobContext.class), eq("지원동기를 작성해주세요."), anyList()))
                .thenReturn(new LlmSelfIntroductionAnswerCompletion(
                        new SelfIntroductionAnswerDocument(List.of(
                                new SelfIntroductionAnswerDocument.Sentence(
                                        "Java API 경험을 바탕으로 기여하겠습니다.",
                                        List.of("C1")
                                )
                        )),
                        new LlmUsage(10, 20, 30),
                        "ollama-answer"
                ));
        when(llmClient.provider()).thenReturn("ollama");
        when(llmClient.model()).thenReturn("llama3.1:8b");
        when(llmClient.selfIntroductionPromptVersion()).thenReturn("self-introduction-v4");

        SelfIntroductionAnswerOutcome outcome = service(questionService, jobPostingService, searchService, llmClient)
                .generate(USER_ID, RESUME_ID, QUESTION_ID);

        assertThat(outcome.answer()).isEqualTo("Java API 경험을 바탕으로 기여하겠습니다 [C1].");
        assertThat(outcome.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.citationId()).isEqualTo("C1");
            assertThat(citation.sourceType()).isEqualTo(SourceType.EXPERIENCE);
        });
        assertThat(outcome.retrievedChunkCount()).isEqualTo(1);
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(searchService).search(eq(USER_ID), query.capture(), eq(8));
        assertThat(query.getValue())
                .contains("지원동기를 작성해주세요.", "Backend Engineer")
                .doesNotContain("Build Java APIs and PostgreSQL services");
    }

    @Test
    void rejectsUnknownCitationBeforeReturningAnswer() {
        ResumeApplicationQuestionService questionService = mock(ResumeApplicationQuestionService.class);
        JobPostingService jobPostingService = mock(JobPostingService.class);
        VectorSearchService searchService = mock(VectorSearchService.class);
        LlmSelfIntroductionAnswerClient llmClient = mock(LlmSelfIntroductionAnswerClient.class);
        ResumeApplicationQuestion applicationQuestion = applicationQuestion("질문");
        when(questionService.get(USER_ID, RESUME_ID, QUESTION_ID))
                .thenReturn(applicationQuestion);
        when(jobPostingService.get(USER_ID, JOB_POSTING_ID)).thenReturn(jobPosting());
        EmbeddingChunkSearchResult evidence = evidence();
        when(searchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(evidence), "nomic-embed-text", 768));
        when(llmClient.generateAnswer(any(SelfIntroductionJobContext.class), anyString(), anyList()))
                .thenReturn(new LlmSelfIntroductionAnswerCompletion(
                        new SelfIntroductionAnswerDocument(List.of(
                                new SelfIntroductionAnswerDocument.Sentence("근거 없는 답변.", List.of("C99"))
                        )),
                        new LlmUsage(1, 2, 3),
                        "ollama-answer"
                ));

        assertThatThrownBy(() -> service(questionService, jobPostingService, searchService, llmClient)
                .generate(USER_ID, RESUME_ID, QUESTION_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.UNKNOWN_CITATION);
    }

    @Test
    void rejectsLowSimilarityEvidenceWithoutCallingTheLanguageModel() {
        ResumeApplicationQuestionService questionService = mock(ResumeApplicationQuestionService.class);
        JobPostingService jobPostingService = mock(JobPostingService.class);
        VectorSearchService searchService = mock(VectorSearchService.class);
        LlmSelfIntroductionAnswerClient llmClient = mock(LlmSelfIntroductionAnswerClient.class);
        ResumeApplicationQuestion applicationQuestion = applicationQuestion("지원동기를 작성해주세요.");
        when(questionService.get(USER_ID, RESUME_ID, QUESTION_ID)).thenReturn(applicationQuestion);
        when(jobPostingService.get(USER_ID, JOB_POSTING_ID)).thenReturn(jobPosting());
        EmbeddingChunkSearchResult weakEvidence = new EmbeddingChunkSearchResult(
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.randomUUID(),
                0,
                "전혀 다른 업무 기록입니다.",
                0.2,
                "nomic-embed-text"
        );
        when(searchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(weakEvidence), "nomic-embed-text", 768));

        assertThatThrownBy(() -> service(questionService, jobPostingService, searchService, llmClient)
                .generate(USER_ID, RESUME_ID, QUESTION_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.EMPTY_CONTEXT);
        verify(llmClient, never()).generateAnswer(any(SelfIntroductionJobContext.class), anyString(), anyList());
    }

    @Test
    void rejectsASentenceWithoutCitationIds() {
        ResumeApplicationQuestionService questionService = mock(ResumeApplicationQuestionService.class);
        JobPostingService jobPostingService = mock(JobPostingService.class);
        VectorSearchService searchService = mock(VectorSearchService.class);
        LlmSelfIntroductionAnswerClient llmClient = mock(LlmSelfIntroductionAnswerClient.class);
        ResumeApplicationQuestion applicationQuestion = applicationQuestion("지원동기를 작성해주세요.");
        when(questionService.get(USER_ID, RESUME_ID, QUESTION_ID)).thenReturn(applicationQuestion);
        when(jobPostingService.get(USER_ID, JOB_POSTING_ID)).thenReturn(jobPosting());
        when(searchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(evidence()), "nomic-embed-text", 768));
        when(llmClient.generateAnswer(any(SelfIntroductionJobContext.class), anyString(), anyList()))
                .thenReturn(new LlmSelfIntroductionAnswerCompletion(
                        new SelfIntroductionAnswerDocument(List.of(
                                new SelfIntroductionAnswerDocument.Sentence(
                                        "저는 탁월한 성과를 만든 인재입니다.",
                                        List.of()
                                )
                        )),
                        new LlmUsage(1, 2, 3),
                        "ollama-answer"
                ));

        assertThatThrownBy(() -> service(questionService, jobPostingService, searchService, llmClient)
                .generate(USER_ID, RESUME_ID, QUESTION_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.UNCITED_FINDING);
    }

    @Test
    void rendersEachSentenceFromACitedEvidenceBlockWithoutLosingItsReferences() {
        ResumeApplicationQuestionService questionService = mock(ResumeApplicationQuestionService.class);
        JobPostingService jobPostingService = mock(JobPostingService.class);
        VectorSearchService searchService = mock(VectorSearchService.class);
        LlmSelfIntroductionAnswerClient llmClient = mock(LlmSelfIntroductionAnswerClient.class);
        ResumeApplicationQuestion applicationQuestion = applicationQuestion("지원동기를 작성해주세요.");
        when(questionService.get(USER_ID, RESUME_ID, QUESTION_ID))
                .thenReturn(applicationQuestion);
        when(jobPostingService.get(USER_ID, JOB_POSTING_ID)).thenReturn(jobPosting());
        when(searchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(evidence()), "nomic-embed-text", 768));
        when(llmClient.generateAnswer(any(SelfIntroductionJobContext.class), anyString(), anyList()))
                .thenReturn(new LlmSelfIntroductionAnswerCompletion(
                        new SelfIntroductionAnswerDocument(List.of(
                                new SelfIntroductionAnswerDocument.Sentence(
                                        "Java API를 개발했습니다. PostgreSQL 서비스도 개발했습니다.",
                                        List.of("C1")
                                )
                        )),
                        new LlmUsage(1, 2, 3),
                        "ollama-answer"
                ));

        var outcome = service(questionService, jobPostingService, searchService, llmClient)
                .generate(USER_ID, RESUME_ID, QUESTION_ID);

        assertThat(outcome.answer()).isEqualTo("Java API를 개발했습니다 [C1]. PostgreSQL 서비스도 개발했습니다 [C1].");
        assertThat(outcome.citations()).singleElement().satisfies(citation ->
                assertThat(citation.citationId()).isEqualTo("C1"));
    }

    @Test
    void prioritizesExplicitPostingSkillsOverGenericSiEvidenceWithAHigherVectorScore() {
        var questionService = mock(ResumeApplicationQuestionService.class);
        var jobPostingService = mock(JobPostingService.class);
        var searchService = mock(VectorSearchService.class);
        var llmClient = mock(LlmSelfIntroductionAnswerClient.class);
        var question = applicationQuestion("지원동기와 입사 후 포부");
        when(questionService.get(USER_ID, RESUME_ID, QUESTION_ID)).thenReturn(question);
        when(jobPostingService.get(USER_ID, JOB_POSTING_ID)).thenReturn(JobPosting.create(
                User.create("test@example.com", "hash", "Test User", java.time.Instant.now()),
                "물류SI", "Example Corp", "WMS, WCS 연동과 Java, Kafka 개발 경험이 필요합니다.",
                null, java.time.Instant.now()
        ));
        UUID projectId = UUID.randomUUID();
        var generic = new EmbeddingChunkSearchResult(UUID.randomUUID(), SourceType.EXPERIENCE,
                UUID.randomUUID(), 0, "일반 SI 및 LMS 웹 기능을 개발했습니다.", 0.96, "nomic-embed-text");
        var direct = new EmbeddingChunkSearchResult(UUID.randomUUID(), SourceType.PROJECT,
                projectId, 0, "Java와 Kafka로 WMS-WES-WCS 비동기 연동을 구현했습니다.", 0.7, "nomic-embed-text");
        when(searchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(generic, direct), "nomic-embed-text", 768));
        when(llmClient.generateAnswer(any(SelfIntroductionJobContext.class), anyString(), anyList()))
                .thenReturn(new LlmSelfIntroductionAnswerCompletion(new SelfIntroductionAnswerDocument(List.of(
                        new SelfIntroductionAnswerDocument.Sentence("Java와 Kafka로 WMS와 WCS를 연동했습니다.", List.of("C1"))
                )), new LlmUsage(10, 20, 30), "answer"));

        var outcome = service(questionService, jobPostingService, searchService, llmClient)
                .generate(USER_ID, RESUME_ID, QUESTION_ID);

        assertThat(outcome.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.sourceType()).isEqualTo(SourceType.PROJECT);
            assertThat(citation.sourceId()).isEqualTo(projectId);
        });
        assertThat(outcome.retrievedChunkCount()).isEqualTo(2);
    }

    private SelfIntroductionAnswerService service(
            ResumeApplicationQuestionService questionService,
            JobPostingService jobPostingService,
            VectorSearchService searchService,
            LlmSelfIntroductionAnswerClient llmClient
    ) {
        return new SelfIntroductionAnswerService(
                questionService,
                jobPostingService,
                searchService,
                llmClient,
                new SelfIntroductionProperties("self-introduction-v4", 8, 0.45, 24000, 1800, 0, true, 2,
                        java.time.Duration.ofSeconds(300), 900)
        );
    }

    private ResumeApplicationQuestion applicationQuestion(String question) {
        ResumeApplicationQuestion applicationQuestion = mock(ResumeApplicationQuestion.class);
        when(applicationQuestion.getQuestion()).thenReturn(question);
        when(applicationQuestion.getJobPostingId()).thenReturn(JOB_POSTING_ID);
        return applicationQuestion;
    }

    private JobPosting jobPosting() {
        return JobPosting.create(
                User.create("test@example.com", "hash", "Test User", java.time.Instant.now()),
                "Backend Engineer",
                "Example Corp",
                "Build Java APIs and PostgreSQL services.",
                null,
                java.time.Instant.now()
        );
    }

    private EmbeddingChunkSearchResult evidence() {
        return new EmbeddingChunkSearchResult(
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.randomUUID(),
                0,
                "회사에서 Java API와 PostgreSQL 서비스를 개발했습니다.",
                0.94,
                "nomic-embed-text"
        );
    }
}
