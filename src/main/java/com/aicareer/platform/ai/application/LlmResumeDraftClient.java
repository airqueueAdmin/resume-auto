package com.aicareer.platform.ai.application;

import java.util.List;

import com.aicareer.platform.job.domain.JobPosting;

public interface LlmResumeDraftClient {

    LlmResumeDraftCompletion generateResumeDraft(JobPosting jobPosting, List<RetrievedContext> contexts);

    String provider();

    String model();

    String resumeDraftPromptVersion();
}
