package com.aicareer.platform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.ai.domain.ResumeDraft;
import com.aicareer.platform.ai.infrastructure.config.ResumeDraftProperties;
import com.aicareer.platform.ai.infrastructure.persistence.ResumeDraftRepository;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.application.VectorSearchOutcome;
import com.aicareer.platform.retrieval.application.VectorSearchService;
import com.aicareer.platform.retrieval.domain.SourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResumeDraftServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

    @Test
    void resolvesEveryDraftLineToRetrievedSourceReferences() {
        JobPostingService jobPostingService = org.mockito.Mockito.mock(JobPostingService.class);
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        VectorSearchService vectorSearchService = org.mockito.Mockito.mock(VectorSearchService.class);
        LlmResumeDraftClient llmClient = org.mockito.Mockito.mock(LlmResumeDraftClient.class);
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        User user = User.create("resume-draft@example.com", "hash", "Resume Draft", Instant.now());
        JobPosting jobPosting = jobPosting();
        EmbeddingChunkSearchResult result = result();
        EmbeddingChunkSearchResult additionalCareerResult = result();

        when(jobPostingService.get(USER_ID, JOB_ID)).thenReturn(jobPosting);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(vectorSearchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(result), "nomic-embed-text", 768));
        when(vectorSearchService.searchAll(USER_ID)).thenReturn(List.of(result, additionalCareerResult));
        when(llmClient.generateResumeDraft(eq(jobPosting), anyList())).thenReturn(new LlmResumeDraftCompletion(
                draft("C1"),
                new LlmUsage(10, 20, 30),
                "ollama-draft"
        ));
        when(llmClient.provider()).thenReturn("ollama");
        when(llmClient.model()).thenReturn("llama3.1:8b");
        when(llmClient.resumeDraftPromptVersion()).thenReturn("resume-draft-v1");
        when(repository.save(any(ResumeDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeDraftOutcome outcome = service(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmClient,
                repository
        ).generate(USER_ID, JOB_ID);

        assertThat(outcome.draft().headline().citations().get(0).citationId()).isEqualTo("C1");
        assertThat(outcome.draft().experienceBullets().get(0).citations().get(0).sourceId())
                .isEqualTo(result.sourceId());
        assertThat(outcome.draft().selfIntroductionSections()).hasSize(3);
        assertThat(outcome.draft().skills()).hasSize(1);
        assertThat(outcome.retrievedChunkCount()).isEqualTo(2);
        verify(repository).save(any(ResumeDraft.class));
    }

    @Test
    void rejectsDraftCitationThatWasNotRetrieved() {
        JobPostingService jobPostingService = org.mockito.Mockito.mock(JobPostingService.class);
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        VectorSearchService vectorSearchService = org.mockito.Mockito.mock(VectorSearchService.class);
        LlmResumeDraftClient llmClient = org.mockito.Mockito.mock(LlmResumeDraftClient.class);
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        JobPosting jobPosting = jobPosting();

        when(jobPostingService.get(USER_ID, JOB_ID)).thenReturn(jobPosting);
        when(vectorSearchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(result()), "nomic-embed-text", 768));
        when(vectorSearchService.searchAll(USER_ID)).thenReturn(List.of(result()));
        when(llmClient.generateResumeDraft(eq(jobPosting), anyList())).thenReturn(new LlmResumeDraftCompletion(
                draft("C99"),
                new LlmUsage(1, 2, 3),
                "ollama-draft"
        ));

        assertThatThrownBy(() -> service(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmClient,
                repository
        ).generate(USER_ID, JOB_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.UNKNOWN_CITATION);
        verify(repository, never()).save(any(ResumeDraft.class));
    }

    @Test
    void rejectsResumeDraftWithFewerThanThreeSelfIntroductionSections() {
        JobPostingService jobPostingService = org.mockito.Mockito.mock(JobPostingService.class);
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        VectorSearchService vectorSearchService = org.mockito.Mockito.mock(VectorSearchService.class);
        LlmResumeDraftClient llmClient = org.mockito.Mockito.mock(LlmResumeDraftClient.class);
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        JobPosting jobPosting = jobPosting();
        EmbeddingChunkSearchResult result = result();
        ResumeDraftDocument fullDraft = draft("C1");
        ResumeDraftDocument shortDraft = new ResumeDraftDocument(
                fullDraft.headline(),
                fullDraft.summary(),
                List.of(fullDraft.selfIntroductionSections().get(0)),
                fullDraft.experienceBullets(),
                fullDraft.projectBullets(),
                fullDraft.skills()
        );

        when(jobPostingService.get(USER_ID, JOB_ID)).thenReturn(jobPosting);
        when(vectorSearchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(result), "nomic-embed-text", 768));
        when(vectorSearchService.searchAll(USER_ID)).thenReturn(List.of(result));
        when(llmClient.generateResumeDraft(eq(jobPosting), anyList())).thenReturn(new LlmResumeDraftCompletion(
                shortDraft,
                new LlmUsage(1, 2, 3),
                "ollama-draft"
        ));

        assertThatThrownBy(() -> service(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmClient,
                repository
        ).generate(USER_ID, JOB_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.INVALID_FINDING);
        verify(repository, never()).save(any(ResumeDraft.class));
    }

    @Test
    void rejectsSelfIntroductionTitleLongerThanJobKoreaLimit() {
        JobPostingService jobPostingService = org.mockito.Mockito.mock(JobPostingService.class);
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        VectorSearchService vectorSearchService = org.mockito.Mockito.mock(VectorSearchService.class);
        LlmResumeDraftClient llmClient = org.mockito.Mockito.mock(LlmResumeDraftClient.class);
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        JobPosting jobPosting = jobPosting();
        EmbeddingChunkSearchResult result = result();
        ResumeDraftDocument fullDraft = draft("C1");
        ResumeDraftSection first = fullDraft.selfIntroductionSections().get(0);
        ResumeDraftDocument invalidDraft = new ResumeDraftDocument(
                fullDraft.headline(),
                fullDraft.summary(),
                List.of(
                        new ResumeDraftSection("가".repeat(51), first.content(), first.citationIds()),
                        fullDraft.selfIntroductionSections().get(1),
                        fullDraft.selfIntroductionSections().get(2)
                ),
                fullDraft.experienceBullets(),
                fullDraft.projectBullets(),
                fullDraft.skills()
        );

        when(jobPostingService.get(USER_ID, JOB_ID)).thenReturn(jobPosting);
        when(vectorSearchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(result), "nomic-embed-text", 768));
        when(vectorSearchService.searchAll(USER_ID)).thenReturn(List.of(result));
        when(llmClient.generateResumeDraft(eq(jobPosting), anyList())).thenReturn(new LlmResumeDraftCompletion(
                invalidDraft,
                new LlmUsage(1, 2, 3),
                "ollama-draft"
        ));

        assertThatThrownBy(() -> service(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmClient,
                repository
        ).generate(USER_ID, JOB_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.INVALID_FINDING);
        verify(repository, never()).save(any(ResumeDraft.class));
    }

    private ResumeDraftService service(
            JobPostingService jobPostingService,
            UserRepository userRepository,
            VectorSearchService vectorSearchService,
            LlmResumeDraftClient llmClient,
            ResumeDraftRepository repository
    ) {
        return new ResumeDraftService(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmClient,
                repository,
                new ObjectMapper().findAndRegisterModules(),
                new ResumeDraftProperties("gemini", "resume-draft-v1", 8, 12000, 450, 512, 1),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private ResumeDraftDocument draft(String citationId) {
        ResumeDraftLine line = new ResumeDraftLine("Built Java APIs", List.of(citationId));
        List<ResumeDraftSection> sections = List.of(
                new ResumeDraftSection(
                        "문제 해결과 실행력",
                        "복잡한 요구사항을 작은 단위로 나누어 핵심 문제를 먼저 정의했습니다. 구현 과정에서는 Java API 구조를 명확히 나누고 검증 가능한 방식으로 작업했습니다. 그 결과 운영 가능한 서비스로 연결하며 문제를 끝까지 해결하는 방식을 체득했습니다.",
                        List.of(citationId)
                ),
                new ResumeDraftSection(
                        "협업과 책임감",
                        "서비스를 혼자 완성하는 것보다 팀이 이해하고 이어서 개선할 수 있는 구조를 만드는 데 집중했습니다. 변경 이유와 사용 방법을 문서화하고 안정적인 API 계약을 지키며 협업했습니다. 이를 통해 개인의 구현을 팀의 지속적인 성과로 확장했습니다.",
                        List.of(citationId)
                ),
                new ResumeDraftSection(
                        "지원 직무와의 연결",
                        "제가 쌓아온 Java API 개발 경험은 백엔드 직무에서 요구하는 안정적인 서비스 구현과 직접 연결됩니다. 기존 경험에서 확인된 기술과 결과를 바탕으로 새로운 요구사항도 빠르게 구조화하고 검증하겠습니다. 입사 후에도 근거 있는 개선으로 서비스 품질을 높이겠습니다.",
                        List.of(citationId)
                )
        );
        return new ResumeDraftDocument(line, line, sections, List.of(line), List.of(), List.of(line));
    }

    private JobPosting jobPosting() {
        User user = User.create("job@example.com", "hash", "Job User", Instant.now());
        return JobPosting.create(
                user,
                "Backend Developer",
                "Example Corp",
                "Build Java APIs and PostgreSQL services.",
                null,
                Instant.now()
        );
    }

    private EmbeddingChunkSearchResult result() {
        return new EmbeddingChunkSearchResult(
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.randomUUID(),
                0,
                "Built Java APIs for production services.",
                0.93,
                "nomic-embed-text"
        );
    }
}
