package com.aicareer.platform.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.ai.domain.JobAnalysis;
import com.aicareer.platform.ai.infrastructure.persistence.JobAnalysisRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.job.domain.JobPosting;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobAnalysisService {

    private final JobPostingService jobPostingService;
    private final UserRepository userRepository;
    private final JobAnalysisRepository jobAnalysisRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JobAnalysisService(
            JobPostingService jobPostingService,
            UserRepository userRepository,
            JobAnalysisRepository jobAnalysisRepository,
            LlmClient llmClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.jobPostingService = jobPostingService;
        this.userRepository = userRepository;
        this.jobAnalysisRepository = jobAnalysisRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public JobAnalysisOutcome analyze(UUID userId, UUID jobPostingId) {
        JobPosting jobPosting = jobPostingService.get(userId, jobPostingId);
        LlmCompletion completion = llmClient.analyzeJob(jobPosting);
        Instant now = clock.instant();

        JobAnalysis saved = jobAnalysisRepository.save(JobAnalysis.create(
                userRepository.getReferenceById(userId),
                jobPosting,
                llmClient.provider(),
                llmClient.model(),
                llmClient.promptVersion(),
                completion.providerResponseId(),
                objectMapper.valueToTree(completion.analysis()),
                completion.usage(),
                now
        ));

        return new JobAnalysisOutcome(
                saved.getId(),
                jobPosting.getId(),
                completion.analysis(),
                saved.getProvider(),
                saved.getModel(),
                saved.getPromptVersion(),
                completion.usage(),
                saved.getCreatedAt()
        );
    }
}
