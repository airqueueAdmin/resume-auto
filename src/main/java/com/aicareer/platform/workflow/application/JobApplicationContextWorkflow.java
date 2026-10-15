package com.aicareer.platform.workflow.application;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aicareer.platform.career.experience.api.ExperienceResponse;
import com.aicareer.platform.career.project.api.ProjectResponse;
import com.aicareer.platform.job.api.JobPostingResponse;
import com.aicareer.platform.tool.application.ToolCall;
import com.aicareer.platform.tool.application.ToolCallExecutor;
import com.aicareer.platform.tool.application.ToolCallResult;
import com.aicareer.platform.tool.application.ToolRegistry;
import com.aicareer.platform.workflow.api.JobApplicationContextRequest;
import com.aicareer.platform.workflow.infrastructure.config.WorkflowProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationContextWorkflow {

    public static final String WORKFLOW_NAME = "job-application-context-v1";
    private static final int READ_TOOL_COST = 1;

    private final ToolCallExecutor toolCallExecutor;
    private final ObjectMapper objectMapper;
    private final WorkflowProperties properties;
    private final java.time.Clock clock;

    public JobApplicationContextWorkflow(
            ToolCallExecutor toolCallExecutor,
            ObjectMapper objectMapper,
            WorkflowProperties properties,
            java.time.Clock clock
    ) {
        this.toolCallExecutor = toolCallExecutor;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public JobApplicationContextOutcome execute(UUID userId, JobApplicationContextRequest request) {
        validateRequest(request);
        validateUniqueIds(request.experienceIds(), "experienceIds");
        validateUniqueIds(request.projectIds(), "projectIds");
        WorkflowBudget budget = new WorkflowBudget(
                clock,
                properties.timeout(),
                properties.maxSteps(),
                properties.maxCostUnits()
        );

        JobPostingResponse jobPosting = read(
                userId,
                ToolRegistry.GET_JOB_POSTING,
                objectMapper.createObjectNode().put("jobPostingId", request.jobPostingId().toString()),
                JobPostingResponse.class,
                budget
        );
        List<ExperienceResponse> experiences = request.experienceIds().stream()
                .map(id -> read(
                        userId,
                        ToolRegistry.GET_EXPERIENCE,
                        objectMapper.createObjectNode().put("experienceId", id.toString()),
                        ExperienceResponse.class,
                        budget
                ))
                .toList();
        List<ProjectResponse> projects = request.projectIds().stream()
                .map(id -> read(
                        userId,
                        ToolRegistry.GET_PROJECT,
                        objectMapper.createObjectNode().put("projectId", id.toString()),
                        ProjectResponse.class,
                        budget
                ))
                .toList();

        return new JobApplicationContextOutcome(
                WORKFLOW_NAME,
                jobPosting,
                experiences,
                projects,
                budget.stepsUsed(),
                budget.costUnits()
        );
    }

    private <T> T read(
            UUID userId,
            String toolName,
            com.fasterxml.jackson.databind.JsonNode arguments,
            Class<T> resultType,
            WorkflowBudget budget
    ) {
        budget.beforeStep(READ_TOOL_COST);
        ToolCallResult result = toolCallExecutor.execute(userId, new ToolCall(toolName, arguments, null));
        budget.afterStep();
        if (result.mutating() || !toolName.equals(result.toolName()) || !resultType.isInstance(result.result())) {
            throw new IllegalStateException("Workflow received an unexpected tool result");
        }
        return resultType.cast(result.result());
    }

    private void validateUniqueIds(List<UUID> ids, String fieldName) {
        if (ids.size() != new HashSet<>(ids).size()) {
            throw new WorkflowArgumentValidationException(Map.of(
                    fieldName, "must not contain duplicate identifiers"
            ));
        }
    }

    private void validateRequest(JobApplicationContextRequest request) {
        if (request == null) {
            throw new WorkflowArgumentValidationException(Map.of(
                    "request", "must not be null"
            ));
        }
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (request.jobPostingId() == null) {
            fieldErrors.put("jobPostingId", "must not be null");
        }
        validateIds(request.experienceIds(), "experienceIds", fieldErrors);
        validateIds(request.projectIds(), "projectIds", fieldErrors);
        if (!fieldErrors.isEmpty()) {
            throw new WorkflowArgumentValidationException(fieldErrors);
        }
    }

    private void validateIds(List<UUID> ids, String fieldName, Map<String, String> fieldErrors) {
        if (ids == null) {
            fieldErrors.put(fieldName, "must not be null");
            return;
        }
        if (ids.stream().anyMatch(id -> id == null)) {
            fieldErrors.put(fieldName, "must not contain null identifiers");
        }
    }
}
