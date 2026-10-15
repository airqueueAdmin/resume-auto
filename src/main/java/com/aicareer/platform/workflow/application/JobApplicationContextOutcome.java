package com.aicareer.platform.workflow.application;

import java.util.List;

import com.aicareer.platform.career.experience.api.ExperienceResponse;
import com.aicareer.platform.career.project.api.ProjectResponse;
import com.aicareer.platform.job.api.JobPostingResponse;

public record JobApplicationContextOutcome(
        String workflowName,
        JobPostingResponse jobPosting,
        List<ExperienceResponse> experiences,
        List<ProjectResponse> projects,
        int stepsUsed,
        int costUnits
) {
    public JobApplicationContextOutcome {
        experiences = List.copyOf(experiences);
        projects = List.copyOf(projects);
    }
}
