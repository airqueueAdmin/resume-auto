package com.aicareer.platform.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.aicareer.platform.ai.domain.ResumeDraft;
import com.aicareer.platform.ai.infrastructure.config.ResumeDraftProperties;
import com.aicareer.platform.ai.infrastructure.persistence.ResumeDraftRepository;
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
public class ResumeDraftService {

    private static final int MAX_RETRIEVAL_QUERY_CHARACTERS = 2000;
    private static final int DEFAULT_SELF_INTRODUCTION_SECTIONS = 3;
    private static final int MAX_SELF_INTRODUCTION_TITLE_CHARACTERS = 50;
    private static final int MIN_SELF_INTRODUCTION_CONTENT_CHARACTERS = 80;
    private static final int MIN_SELF_INTRODUCTION_SENTENCES = 3;

    private final JobPostingService jobPostingService;
    private final UserRepository userRepository;
    private final VectorSearchService vectorSearchService;
    private final LlmResumeDraftClient llmResumeDraftClient;
    private final ResumeDraftRepository resumeDraftRepository;
    private final ObjectMapper objectMapper;
    private final ResumeDraftProperties properties;
    private final Clock clock;

    public ResumeDraftService(
            JobPostingService jobPostingService,
            UserRepository userRepository,
            VectorSearchService vectorSearchService,
            LlmResumeDraftClient llmResumeDraftClient,
            ResumeDraftRepository resumeDraftRepository,
            ObjectMapper objectMapper,
            ResumeDraftProperties properties,
            Clock clock
    ) {
        this.jobPostingService = jobPostingService;
        this.userRepository = userRepository;
        this.vectorSearchService = vectorSearchService;
        this.llmResumeDraftClient = llmResumeDraftClient;
        this.resumeDraftRepository = resumeDraftRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ResumeDraftOutcome generate(UUID userId, UUID jobPostingId) {
        JobPosting jobPosting = jobPostingService.get(userId, jobPostingId);
        GeneratedDraft generated = generateDraft(userId, jobPosting);
        Instant now = clock.instant();

        List<SourceCitation> sourceReferences = sourceReferences(generated.groundedDraft());
        ResumeDraft saved = resumeDraftRepository.save(ResumeDraft.create(
                userRepository.getReferenceById(userId),
                jobPosting,
                llmResumeDraftClient.provider(),
                llmResumeDraftClient.model(),
                llmResumeDraftClient.resumeDraftPromptVersion(),
                generated.completion().providerResponseId(),
                objectMapper.valueToTree(generated.groundedDraft()),
                objectMapper.valueToTree(sourceReferences),
                generated.contexts().size(),
                generated.completion().usage(),
                now
        ));

        return new ResumeDraftOutcome(
                saved.getId(),
                jobPosting.getId(),
                generated.groundedDraft(),
                saved.getProvider(),
                saved.getModel(),
                saved.getPromptVersion(),
                generated.completion().usage(),
                saved.getRetrievedChunkCount(),
                saved.getCreatedAt()
        );
    }

    /**
     * Generates a review-only draft from a JobKorea page without creating a JobPosting or ResumeDraft.
     */
    @Transactional(readOnly = true)
    public PageResumeDraftOutcome generateForPage(
            UUID userId,
            String companyName,
            String jobTitle,
            String jobDescription,
            String sourceUrl
    ) {
        JobPosting pagePosting = JobPosting.create(
                userRepository.getReferenceById(userId),
                jobTitle.strip(),
                companyName.strip(),
                jobDescription.strip(),
                sourceUrl.strip(),
                clock.instant()
        );
        GeneratedDraft generated = generateDraft(userId, pagePosting);
        return new PageResumeDraftOutcome(
                generated.groundedDraft(),
                llmResumeDraftClient.provider(),
                llmResumeDraftClient.model(),
                llmResumeDraftClient.resumeDraftPromptVersion(),
                generated.completion().usage(),
                generated.contexts().size(),
                clock.instant()
        );
    }

    private GeneratedDraft generateDraft(UUID userId, JobPosting jobPosting) {
        VectorSearchOutcome retrieval = vectorSearchService.search(
                userId,
                retrievalQuery(jobPosting),
                properties.topK()
        );
        List<EmbeddingChunkSearchResult> evidence = mergeEvidence(
                retrieval.results(),
                vectorSearchService.searchAll(userId)
        );
        List<RetrievedContext> contexts = buildContexts(evidence);
        if (contexts.isEmpty()) {
            throw GroundingValidationException.emptyContext();
        }

        LlmResumeDraftCompletion completion = llmResumeDraftClient.generateResumeDraft(jobPosting, contexts);
        GroundedResumeDraftDocument groundedDraft = ground(completion.draft(), contexts);
        return new GeneratedDraft(groundedDraft, completion, contexts);
    }

    private record GeneratedDraft(
            GroundedResumeDraftDocument groundedDraft,
            LlmResumeDraftCompletion completion,
            List<RetrievedContext> contexts
    ) {
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

    private List<EmbeddingChunkSearchResult> mergeEvidence(
            List<EmbeddingChunkSearchResult> relevant,
            List<EmbeddingChunkSearchResult> allCareerEvidence
    ) {
        Map<UUID, EmbeddingChunkSearchResult> byChunkId = new LinkedHashMap<>();
        if (relevant != null) {
            relevant.forEach(result -> byChunkId.put(result.chunkId(), result));
        }
        if (allCareerEvidence != null) {
            allCareerEvidence.forEach(result -> byChunkId.putIfAbsent(result.chunkId(), result));
        }
        return List.copyOf(byChunkId.values());
    }

    private GroundedResumeDraftDocument ground(ResumeDraftDocument draft, List<RetrievedContext> contexts) {
        Map<String, RetrievedContext> byCitationId = contexts.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RetrievedContext::citationId,
                        context -> context,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return new GroundedResumeDraftDocument(
                groundLine(draft.headline(), byCitationId),
                groundLine(draft.summary(), byCitationId),
                groundSections(draft.selfIntroductionSections(), byCitationId),
                groundLines(draft.experienceBullets(), byCitationId),
                groundLines(draft.projectBullets(), byCitationId),
                groundLines(draft.skills(), byCitationId)
        );
    }

    private List<GroundedResumeDraftSection> groundSections(
            List<ResumeDraftSection> sections,
            Map<String, RetrievedContext> byCitationId
    ) {
        if (sections == null || sections.size() != DEFAULT_SELF_INTRODUCTION_SECTIONS) {
            throw GroundingValidationException.invalidFinding();
        }

        Set<String> titles = new HashSet<>();
        List<GroundedResumeDraftSection> groundedSections = sections.stream()
                .map(section -> groundSection(section, byCitationId, titles))
                .toList();
        if (groundedSections.size() != DEFAULT_SELF_INTRODUCTION_SECTIONS) {
            throw GroundingValidationException.invalidFinding();
        }
        return groundedSections;
    }

    private GroundedResumeDraftSection groundSection(
            ResumeDraftSection section,
            Map<String, RetrievedContext> byCitationId,
            Set<String> titles
    ) {
        if (section == null || section.title() == null || section.title().isBlank()
                || section.title().trim().length() > MAX_SELF_INTRODUCTION_TITLE_CHARACTERS
                || section.content() == null || section.content().isBlank()
                || section.content().trim().length() < MIN_SELF_INTRODUCTION_CONTENT_CHARACTERS
                || countSentences(section.content()) < MIN_SELF_INTRODUCTION_SENTENCES) {
            throw GroundingValidationException.invalidFinding();
        }

        String title = section.title().trim();
        if (!titles.add(title.toLowerCase(Locale.ROOT))) {
            throw GroundingValidationException.invalidFinding();
        }
        List<SourceCitation> citations = section.citationIds().stream()
                .map(citationId -> citation(byCitationId, citationId))
                .distinct()
                .toList();
        if (citations.isEmpty()) {
            throw GroundingValidationException.uncitedFinding();
        }
        return new GroundedResumeDraftSection(title, section.content().trim(), citations);
    }

    private long countSentences(String content) {
        return content.chars()
                .filter(character -> character == '.' || character == '!' || character == '?'
                        || character == '\u3002')
                .count();
    }

    private List<GroundedResumeDraftLine> groundLines(
            List<ResumeDraftLine> lines,
            Map<String, RetrievedContext> byCitationId
    ) {
        return lines.stream().map(line -> groundLine(line, byCitationId)).toList();
    }

    private GroundedResumeDraftLine groundLine(
            ResumeDraftLine line,
            Map<String, RetrievedContext> byCitationId
    ) {
        if (line == null || line.text() == null || line.text().isBlank()) {
            throw GroundingValidationException.invalidFinding();
        }
        List<SourceCitation> citations = line.citationIds().stream()
                .map(citationId -> citation(byCitationId, citationId))
                .distinct()
                .toList();
        if (citations.isEmpty()) {
            throw GroundingValidationException.uncitedFinding();
        }
        return new GroundedResumeDraftLine(line.text().trim(), citations);
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

    private List<SourceCitation> sourceReferences(GroundedResumeDraftDocument draft) {
        return Stream.of(
                        Stream.of(draft.headline(), draft.summary()),
                        draft.experienceBullets().stream(),
                        draft.projectBullets().stream(),
                        draft.skills().stream()
                )
                .flatMap(stream -> stream)
                .flatMap(line -> line.citations().stream())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                SourceCitation::citationId,
                                citation -> citation,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        citations -> {
                            Map<String, SourceCitation> all = new LinkedHashMap<>();
                            draft.selfIntroductionSections().stream()
                                    .flatMap(section -> section.citations().stream())
                                    .forEach(citation -> all.putIfAbsent(citation.citationId(), citation));
                            citations.forEach((citationId, citation) -> all.putIfAbsent(citationId, citation));
                            return List.copyOf(all.values());
                        }
                ));
    }
}
