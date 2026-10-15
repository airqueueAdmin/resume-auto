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

import com.aicareer.platform.ai.domain.JobMatch;
import com.aicareer.platform.ai.infrastructure.config.RagProperties;
import com.aicareer.platform.ai.infrastructure.persistence.JobMatchRepository;
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

class RagMatchingServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

    @Test
    void rejectsCitationThatWasNotReturnedByRetrieval() {
        JobPostingService jobPostingService = org.mockito.Mockito.mock(JobPostingService.class);
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        VectorSearchService vectorSearchService = org.mockito.Mockito.mock(VectorSearchService.class);
        LlmMatchClient llmMatchClient = org.mockito.Mockito.mock(LlmMatchClient.class);
        JobMatchRepository jobMatchRepository = org.mockito.Mockito.mock(JobMatchRepository.class);
        JobPosting jobPosting = jobPosting();
        EmbeddingChunkSearchResult result = result("C1");

        when(jobPostingService.get(USER_ID, JOB_ID)).thenReturn(jobPosting);
        when(vectorSearchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(result), "nomic-embed-text", 768));
        when(llmMatchClient.matchJob(eq(jobPosting), anyList())).thenReturn(new LlmMatchCompletion(
                new JobMatchDocument(
                        "Match",
                        List.of(new MatchFinding("Unsupported claim", List.of("C99"))),
                        List.of(),
                        List.of()
                ),
                new LlmUsage(1, 2, 3),
                "ollama-test"
        ));

        RagMatchingService service = service(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmMatchClient,
                jobMatchRepository
        );

        assertThatThrownBy(() -> service.match(USER_ID, JOB_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.UNKNOWN_CITATION);
        verify(jobMatchRepository, never()).save(any(JobMatch.class));
    }

    @Test
    void resolvesValidCitationToImmutableSourceReference() {
        JobPostingService jobPostingService = org.mockito.Mockito.mock(JobPostingService.class);
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        VectorSearchService vectorSearchService = org.mockito.Mockito.mock(VectorSearchService.class);
        LlmMatchClient llmMatchClient = org.mockito.Mockito.mock(LlmMatchClient.class);
        JobMatchRepository jobMatchRepository = org.mockito.Mockito.mock(JobMatchRepository.class);
        User user = User.create("rag-test@example.com", "hash", "RAG Test", Instant.now());
        JobPosting jobPosting = jobPosting();
        EmbeddingChunkSearchResult result = result("C1");

        when(jobPostingService.get(USER_ID, JOB_ID)).thenReturn(jobPosting);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(vectorSearchService.search(eq(USER_ID), anyString(), eq(8)))
                .thenReturn(new VectorSearchOutcome(List.of(result), "nomic-embed-text", 768));
        when(llmMatchClient.matchJob(eq(jobPosting), anyList())).thenReturn(new LlmMatchCompletion(
                new JobMatchDocument(
                        "Strong match",
                        List.of(new MatchFinding("Java API evidence", List.of("C1"))),
                        List.of(new MatchFinding("Cloud certification is not shown", List.of())),
                        List.of(new MatchFinding("Highlight API ownership", List.of("C1")))
                ),
                new LlmUsage(10, 20, 30),
                "ollama-test"
        ));
        when(llmMatchClient.provider()).thenReturn("ollama");
        when(llmMatchClient.model()).thenReturn("llama3.1:8b");
        when(llmMatchClient.matchingPromptVersion()).thenReturn("job-match-v1");
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobMatchOutcome outcome = service(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmMatchClient,
                jobMatchRepository
        ).match(USER_ID, JOB_ID);

        assertThat(outcome.analysis().strengths().get(0).citations().get(0).citationId()).isEqualTo("C1");
        assertThat(outcome.analysis().strengths().get(0).citations().get(0).sourceId())
                .isEqualTo(result.sourceId());
        assertThat(outcome.retrievedChunkCount()).isEqualTo(1);
        verify(jobMatchRepository).save(any(JobMatch.class));
    }

    private RagMatchingService service(
            JobPostingService jobPostingService,
            UserRepository userRepository,
            VectorSearchService vectorSearchService,
            LlmMatchClient llmMatchClient,
            JobMatchRepository jobMatchRepository
    ) {
        return new RagMatchingService(
                jobPostingService,
                userRepository,
                vectorSearchService,
                llmMatchClient,
                jobMatchRepository,
                new ObjectMapper().findAndRegisterModules(),
                new RagProperties("job-match-v1", 8, 12000),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private JobPosting jobPosting() {
        User user = User.create("job@example.com", "hash", "Job User", Instant.now());
        JobPosting posting = JobPosting.create(
                user,
                "Backend Developer",
                "Example Corp",
                "Build Java APIs and PostgreSQL services.",
                null,
                Instant.now()
        );
        return posting;
    }

    private EmbeddingChunkSearchResult result(String citationId) {
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
