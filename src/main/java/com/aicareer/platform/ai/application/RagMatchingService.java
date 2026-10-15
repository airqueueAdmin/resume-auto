package com.aicareer.platform.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.aicareer.platform.ai.domain.JobMatch;
import com.aicareer.platform.ai.infrastructure.config.RagProperties;
import com.aicareer.platform.ai.infrastructure.persistence.JobMatchRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.application.VectorSearchOutcome;
import com.aicareer.platform.retrieval.application.VectorSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagMatchingService {

    private static final int MAX_RETRIEVAL_QUERY_CHARACTERS = 2000;

    private final JobPostingService jobPostingService;
    private final UserRepository userRepository;
    private final VectorSearchService vectorSearchService;
    private final LlmMatchClient llmMatchClient;
    private final JobMatchRepository jobMatchRepository;
    private final ObjectMapper objectMapper;
    private final RagProperties properties;
    private final Clock clock;

    public RagMatchingService(
            JobPostingService jobPostingService,
            UserRepository userRepository,
            VectorSearchService vectorSearchService,
            LlmMatchClient llmMatchClient,
            JobMatchRepository jobMatchRepository,
            ObjectMapper objectMapper,
            RagProperties properties,
            Clock clock
    ) {
        this.jobPostingService = jobPostingService;
        this.userRepository = userRepository;
        this.vectorSearchService = vectorSearchService;
        this.llmMatchClient = llmMatchClient;
        this.jobMatchRepository = jobMatchRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public JobMatchOutcome match(UUID userId, UUID jobPostingId) {
        JobPosting jobPosting = jobPostingService.get(userId, jobPostingId);
        VectorSearchOutcome retrieval = vectorSearchService.search(
                userId,
                retrievalQuery(jobPosting),
                properties.topK()
        );
        List<RetrievedContext> contexts = buildContexts(retrieval.results());
        if (contexts.isEmpty()) {
            throw GroundingValidationException.emptyContext();
        }

        LlmMatchCompletion completion = llmMatchClient.matchJob(jobPosting, contexts);
        GroundedJobMatchDocument groundedAnalysis = ground(completion.analysis(), contexts);
        List<SourceCitation> sourceReferences = sourceReferences(groundedAnalysis);
        Instant now = clock.instant();

        JobMatch saved = jobMatchRepository.save(JobMatch.create(
                userRepository.getReferenceById(userId),
                jobPosting,
                llmMatchClient.provider(),
                llmMatchClient.model(),
                llmMatchClient.matchingPromptVersion(),
                completion.providerResponseId(),
                objectMapper.valueToTree(groundedAnalysis),
                objectMapper.valueToTree(sourceReferences),
                contexts.size(),
                completion.usage(),
                now
        ));

        return new JobMatchOutcome(
                saved.getId(),
                jobPosting.getId(),
                groundedAnalysis,
                saved.getProvider(),
                saved.getModel(),
                saved.getPromptVersion(),
                completion.usage(),
                saved.getRetrievedChunkCount(),
                saved.getCreatedAt()
        );
    }

    private String retrievalQuery(JobPosting jobPosting) {
        String query = "Title: " + jobPosting.getTitle()
                + "\nCompany: " + jobPosting.getCompanyName()
                + "\nJob description:\n" + jobPosting.getDescription();
        return query.length() <= MAX_RETRIEVAL_QUERY_CHARACTERS
                ? query
                : query.substring(0, MAX_RETRIEVAL_QUERY_CHARACTERS);
    }

    private List<RetrievedContext> buildContexts(List<EmbeddingChunkSearchResult> results) {
        int remaining = Math.max(0, properties.maxContextCharacters());
        List<RetrievedContext> contexts = new ArrayList<>();

        for (int index = 0; index < results.size(); index++) {
            EmbeddingChunkSearchResult result = results.get(index);
            String citationId = "C" + (index + 1);
            String header = "[" + citationId + "] source_type=" + result.sourceType()
                    + " similarity=" + result.similarity() + "\n"
                    + "<career_evidence>\n";
            String footer = "\n</career_evidence>\n";
            int contentBudget = remaining - header.length() - footer.length();
            if (contentBudget <= 0) {
                break;
            }

            String promptContent = result.content().length() <= contentBudget
                    ? result.content()
                    : result.content().substring(0, contentBudget);
            if (promptContent.isBlank()) {
                break;
            }

            contexts.add(new RetrievedContext(
                    citationId,
                    result.chunkId(),
                    result.sourceType(),
                    result.sourceId(),
                    result.chunkIndex(),
                    result.content(),
                    promptContent,
                    result.similarity(),
                    result.embeddingModel()
            ));
            remaining -= header.length() + promptContent.length() + footer.length();
        }
        return contexts;
    }

    private GroundedJobMatchDocument ground(JobMatchDocument analysis, List<RetrievedContext> contexts) {
        Map<String, RetrievedContext> byCitationId = contexts.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RetrievedContext::citationId,
                        context -> context,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return new GroundedJobMatchDocument(
                requireStatement(analysis.summary()),
                groundFindings(analysis.strengths(), byCitationId, true),
                groundFindings(analysis.gaps(), byCitationId, false),
                groundFindings(analysis.recommendations(), byCitationId, true)
        );
    }

    private List<GroundedMatchFinding> groundFindings(
            List<MatchFinding> findings,
            Map<String, RetrievedContext> byCitationId,
            boolean citationRequired
    ) {
        return findings.stream()
                .map(finding -> {
                    String statement = requireStatement(finding.statement());
                    List<SourceCitation> citations = finding.citationIds().stream()
                            .map(citationId -> citation(byCitationId, citationId))
                            .distinct()
                            .toList();
                    if (citationRequired && citations.isEmpty()) {
                        throw GroundingValidationException.uncitedFinding();
                    }
                    return new GroundedMatchFinding(statement, citations);
                })
                .toList();
    }

    private String requireStatement(String statement) {
        if (statement == null || statement.isBlank()) {
            throw GroundingValidationException.invalidFinding();
        }
        return statement.trim();
    }

    private SourceCitation citation(Map<String, RetrievedContext> byCitationId, String citationId) {
        RetrievedContext context = byCitationId.get(citationId);
        if (context == null) {
            throw GroundingValidationException.unknownCitation(citationId);
        }
        return new SourceCitation(
                context.citationId(),
                context.chunkId(),
                context.sourceType(),
                context.sourceId(),
                context.chunkIndex(),
                context.content(),
                context.similarity(),
                context.embeddingModel()
        );
    }

    private List<SourceCitation> sourceReferences(GroundedJobMatchDocument analysis) {
        return Stream.of(analysis.strengths(), analysis.gaps(), analysis.recommendations())
                .flatMap(List::stream)
                .flatMap(finding -> finding.citations().stream())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                SourceCitation::citationId,
                                citation -> citation,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        citations -> List.copyOf(citations.values())
                ));
    }
}
