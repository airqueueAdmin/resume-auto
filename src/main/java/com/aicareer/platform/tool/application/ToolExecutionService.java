package com.aicareer.platform.tool.application;

import java.util.UUID;

import com.aicareer.platform.ai.application.ResumeDraftApprovalService;
import com.aicareer.platform.career.experience.api.ExperienceResponse;
import com.aicareer.platform.career.experience.api.ExperienceRequest;
import com.aicareer.platform.career.experience.application.ExperienceService;
import com.aicareer.platform.career.project.api.ProjectResponse;
import com.aicareer.platform.career.project.api.ProjectRequest;
import com.aicareer.platform.career.project.application.ProjectService;
import com.aicareer.platform.career.resume.api.ResumeVersionResponse;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.job.api.JobPostingResponse;
import com.aicareer.platform.job.api.JobPostingRequest;
import com.aicareer.platform.job.application.JobPostingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolExecutionService implements ToolCallExecutor {

    private final ToolRegistry toolRegistry;
    private final ToolApprovalService toolApprovalService;
    private final ExperienceService experienceService;
    private final ProjectService projectService;
    private final JobPostingService jobPostingService;
    private final ResumeDraftApprovalService resumeDraftApprovalService;
    private final ObjectMapper objectMapper;

    public ToolExecutionService(
            ToolRegistry toolRegistry,
            ToolApprovalService toolApprovalService,
            ExperienceService experienceService,
            ProjectService projectService,
            JobPostingService jobPostingService,
            ResumeDraftApprovalService resumeDraftApprovalService,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = toolRegistry;
        this.toolApprovalService = toolApprovalService;
        this.experienceService = experienceService;
        this.projectService = projectService;
        this.jobPostingService = jobPostingService;
        this.resumeDraftApprovalService = resumeDraftApprovalService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public ToolCallResult execute(UUID userId, ToolCall toolCall) {
        ValidatedToolCall call = toolRegistry.validate(toolCall.toolName(), toolCall.arguments());
        if (call.definition().mutating()) {
            toolApprovalService.consume(userId, toolCall.approvalToken(), call);
        }

        Object result = switch (call.definition().name()) {
            case ToolRegistry.GET_EXPERIENCE -> result(ExperienceResponse.from(experienceService.get(
                    userId,
                    parse(call.arguments(), GetExperienceToolArguments.class).experienceId()
            )));
            case ToolRegistry.GET_PROJECT -> result(ProjectResponse.from(projectService.get(
                    userId,
                    parse(call.arguments(), GetProjectToolArguments.class).projectId()
            )));
            case ToolRegistry.GET_JOB_POSTING -> result(JobPostingResponse.from(jobPostingService.get(
                    userId,
                    parse(call.arguments(), GetJobPostingToolArguments.class).jobPostingId()
            )));
            case ToolRegistry.CREATE_EXPERIENCE -> {
                CreateExperienceToolArguments toolArguments = parse(
                        call.arguments(),
                        CreateExperienceToolArguments.class
                );
                yield result(ExperienceResponse.from(experienceService.create(
                        userId,
                        new ExperienceRequest(
                                toolArguments.companyName(),
                                toolArguments.role(),
                                toolArguments.description()
                        )
                )));
            }
            case ToolRegistry.CREATE_JOB_POSTING -> {
                CreateJobPostingToolArguments toolArguments = parse(
                        call.arguments(),
                        CreateJobPostingToolArguments.class
                );
                yield result(JobPostingResponse.from(jobPostingService.create(
                        userId,
                        new JobPostingRequest(
                                toolArguments.title(),
                                toolArguments.companyName(),
                                toolArguments.description(),
                                toolArguments.sourceUrl()
                        )
                )));
            }
            case ToolRegistry.CREATE_PROJECT -> {
                CreateProjectToolArguments toolArguments = parse(
                        call.arguments(),
                        CreateProjectToolArguments.class
                );
                yield result(ProjectResponse.from(projectService.create(
                        userId,
                        new ProjectRequest(
                                toolArguments.name(),
                                toolArguments.role(),
                                toolArguments.startDate(),
                                toolArguments.endDate(),
                                toolArguments.description(),
                                toolArguments.techStack(),
                                toolArguments.repositoryUrl()
                        )
                )));
            }
            case ToolRegistry.APPROVE_RESUME_DRAFT -> {
                ApproveResumeDraftToolArguments toolArguments = parse(
                        call.arguments(),
                        ApproveResumeDraftToolArguments.class
                );
                ResumeVersion version = resumeDraftApprovalService.approve(
                        userId,
                        toolArguments.draftId(),
                        toolArguments.resumeId()
                );
                yield result(ResumeVersionResponse.from(version));
            }
            default -> throw new ToolNotFoundException();
        };
        return new ToolCallResult(call.definition().name(), call.definition().mutating(), result);
    }

    private <T> T parse(JsonNode arguments, Class<T> type) {
        try {
            return objectMapper.treeToValue(arguments, type);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ToolArgumentValidationException(java.util.Map.of(
                    "arguments", "does not match the tool input schema"
            ));
        }
    }

    private Object result(Object value) {
        return value;
    }

}
