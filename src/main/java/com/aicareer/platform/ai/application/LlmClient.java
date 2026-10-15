package com.aicareer.platform.ai.application;

import com.aicareer.platform.job.domain.JobPosting;

public interface LlmClient {

    LlmCompletion analyzeJob(JobPosting jobPosting);

    String provider();

    String model();

    String promptVersion();
}
