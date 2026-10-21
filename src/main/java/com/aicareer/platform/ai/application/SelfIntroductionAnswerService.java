package com.aicareer.platform.ai.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.aicareer.platform.ai.infrastructure.config.SelfIntroductionProperties;
import com.aicareer.platform.career.resume.question.application.ResumeApplicationQuestionService;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.application.VectorSearchOutcome;
import com.aicareer.platform.retrieval.application.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SelfIntroductionAnswerService {

    private static final Logger log = LoggerFactory.getLogger(SelfIntroductionAnswerService.class);
    private static final int MAX_RETRIEVAL_QUERY_CHARACTERS = 3000;
    private static final int MAX_JOB_DESCRIPTION_CHARACTERS = 8000;
    private static final Pattern POSTING_TERM = Pattern.compile("[A-Za-z][A-Za-z0-9]*(?:[.+#-][A-Za-z0-9]+)*[+#]*");
    private static final Set<String> GENERIC_TERMS = Set.of(
            "a", "an", "and", "or", "the", "for", "to", "of", "in", "on", "with", "it", "sw", "pool", "si"
    );

    private final ResumeApplicationQuestionService questionService;
    private final JobPostingService jobPostingService;
    private final VectorSearchService vectorSearchService;
    private final LlmSelfIntroductionAnswerClient llmClient;
    private final SelfIntroductionProperties properties;

    public SelfIntroductionAnswerService(
            ResumeApplicationQuestionService questionService,
            JobPostingService jobPostingService,
            VectorSearchService vectorSearchService,
            @Qualifier("selfIntroductionAnswerLlmClient") LlmSelfIntroductionAnswerClient llmClient,
            SelfIntroductionProperties properties
    ) {
        this.questionService = questionService;
        this.jobPostingService = jobPostingService;
        this.vectorSearchService = vectorSearchService;
        this.llmClient = llmClient;
        this.properties = properties;
    }

    public SelfIntroductionAnswerOutcome generate(UUID userId, UUID resumeId, UUID questionId) {
        var applicationQuestion = questionService.get(userId, resumeId, questionId);
        if (applicationQuestion.getJobPostingId() == null) {
            throw GroundingValidationException.jobPostingRequired();
        }
        JobPosting jobPosting = jobPostingService.get(userId, applicationQuestion.getJobPostingId());
        String question = applicationQuestion.getQuestion();
        VectorSearchOutcome retrieval = vectorSearchService.search(
                userId,
                retrievalQuery(jobPosting, question),
                properties.topK()
        );
        List<EmbeddingChunkSearchResult> relevantResults = retrieval.results().stream()
                .filter(result -> result.similarity() >= properties.minimumSimilarity())
                .toList();
        List<RetrievedContext> contexts = buildContexts(prioritizePostingEvidence(jobPosting, relevantResults));
        log.info("Self-introduction evidence retrievedChunks={} acceptedChunks={} minimumSimilarity={} bestSimilarity={}",
                retrieval.results().size(),
                contexts.size(),
                properties.minimumSimilarity(),
                retrieval.results().stream()
                        .mapToDouble(EmbeddingChunkSearchResult::similarity)
                        .max()
                        .orElse(0.0));
        if (contexts.isEmpty()) {
            throw GroundingValidationException.emptyContext();
        }

        long generationStarted = System.nanoTime();
        LlmSelfIntroductionAnswerCompletion completion = llmClient.generateAnswer(
                jobContext(jobPosting),
                question,
                contexts
        );
        SelfIntroductionAnswerValidator.validate(completion, contexts);
        Map<String, RetrievedContext> byCitationId = contexts.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RetrievedContext::citationId,
                        context -> context,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        List<String> renderedSentences = new ArrayList<>();
        LinkedHashSet<String> usedCitationIds = new LinkedHashSet<>();
        for (SelfIntroductionAnswerDocument.Sentence sentence : completion.answer().sentences()) {
            LinkedHashSet<String> sentenceCitationIds = new LinkedHashSet<>();
            for (String citationId : sentence.citationIds()) {
                citation(byCitationId, citationId);
                sentenceCitationIds.add(citationId);
                usedCitationIds.add(citationId);
            }
            for (String sentenceText : SelfIntroductionAnswerValidator.sentenceTexts(sentence.text())) {
                renderedSentences.add(renderSentence(sentenceText, sentenceCitationIds));
            }
        }

        List<SourceCitation> citations = usedCitationIds.stream()
                .map(citationId -> citation(byCitationId, citationId))
                .distinct()
                .toList();
        if (citations.isEmpty()) {
            throw GroundingValidationException.uncitedFinding();
        }

        String answer = String.join(" ", renderedSentences);
        log.info("Self-introduction generated provider={} model={} promptVersion={} answerCharacters={} citedSources={} inputTokens={} outputTokens={} durationMs={}",
                llmClient.provider(), llmClient.model(), llmClient.selfIntroductionPromptVersion(),
                answer.length(), citations.size(), completion.usage().inputTokens(), completion.usage().outputTokens(),
                (System.nanoTime() - generationStarted) / 1_000_000);
        return new SelfIntroductionAnswerOutcome(
                answer,
                citations,
                llmClient.provider(),
                llmClient.model(),
                llmClient.selfIntroductionPromptVersion(),
                completion.usage(),
                contexts.size()
        );
    }

    private String retrievalQuery(JobPosting jobPosting, String question) {
        String query = "자기소개서 질문: " + question + "\n지원 직무: " + jobPosting.getTitle();
        return query.length() <= MAX_RETRIEVAL_QUERY_CHARACTERS
                ? query
                : query.substring(0, MAX_RETRIEVAL_QUERY_CHARACTERS);
    }

    private List<EmbeddingChunkSearchResult> prioritizePostingEvidence(
            JobPosting posting, List<EmbeddingChunkSearchResult> results
    ) {
        var terms = new LinkedHashSet<String>();
        var matcher = POSTING_TERM.matcher(posting.getTitle() + "\n" + posting.getDescription());
        while (matcher.find() && terms.size() < 32) {
            String term = matcher.group().toLowerCase(Locale.ROOT);
            if (term.length() > 1 && !GENERIC_TERMS.contains(term)) terms.add(term);
        }
        if (terms.isEmpty()) return results;
        List<Pattern> patterns = terms.stream()
                .map(term -> Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])"))
                .toList();
        return results.stream().sorted(Comparator
                .comparingLong((EmbeddingChunkSearchResult result) -> {
                    String content = result.content().toLowerCase(Locale.ROOT);
                    return patterns.stream().filter(pattern -> pattern.matcher(content).find()).count();
                }).reversed()
                .thenComparing(Comparator.comparingDouble(EmbeddingChunkSearchResult::similarity).reversed()))
                .toList();
    }

    private SelfIntroductionJobContext jobContext(JobPosting jobPosting) {
        String description = jobPosting.getDescription();
        String boundedDescription = description.length() <= MAX_JOB_DESCRIPTION_CHARACTERS
                ? description
                : description.substring(0, MAX_JOB_DESCRIPTION_CHARACTERS);
        return new SelfIntroductionJobContext(
                jobPosting.getCompanyName(),
                jobPosting.getTitle(),
                boundedDescription
        );
    }

    private List<RetrievedContext> buildContexts(List<EmbeddingChunkSearchResult> results) {
        int remaining = Math.max(0, properties.maxContextCharacters());
        List<RetrievedContext> contexts = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            EmbeddingChunkSearchResult result = results.get(index);
            String citationId = "C" + (index + 1);
            String header = "[" + citationId + "] source_type=" + result.sourceType()
                    + " similarity=" + result.similarity() + "\n<career_evidence>\n";
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

    private String renderSentence(String text, LinkedHashSet<String> citationIds) {
        String normalized = text.strip();
        String markers = citationIds.stream()
                .map(citationId -> "[" + citationId + "]")
                .collect(java.util.stream.Collectors.joining(" "));
        char lastCharacter = normalized.charAt(normalized.length() - 1);
        if (".!?。！？".indexOf(lastCharacter) >= 0) {
            return normalized.substring(0, normalized.length() - 1).stripTrailing()
                    + " " + markers + lastCharacter;
        }
        return normalized + " " + markers + ".";
    }
}
