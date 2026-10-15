package com.aicareer.platform.workflow.api;

import java.util.List;

import com.aicareer.platform.career.experience.api.ExperienceResponse;
import com.aicareer.platform.career.project.api.ProjectResponse;
import com.aicareer.platform.job.api.JobPostingResponse;
import com.aicareer.platform.workflow.application.JobApplicationContextOutcome;

public record JobApplicationContextResponse(
        String workflowName,
        JobPostingResponse jobPosting,
        List<ExperienceResponse> experiences,
        List<ProjectResponse> projects,
        int stepsUsed,
        int costUnits
) {

    public static JobApplicationContextResponse from(JobApplicationContextOutcome outcome) {
        return new JobApplicationContextResponse(
                outcome.workflowName(),
                outcome.jobPosting(),
                outcome.experiences(),
                outcome.projects(),
                outcome.stepsUsed(),
                outcome.costUnits()
        );
    }
}
