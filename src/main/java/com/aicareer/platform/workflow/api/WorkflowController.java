package com.aicareer.platform.workflow.api;

import java.util.UUID;

import com.aicareer.platform.workflow.application.JobApplicationContextWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {

    private final JobApplicationContextWorkflow jobApplicationContextWorkflow;

    public WorkflowController(JobApplicationContextWorkflow jobApplicationContextWorkflow) {
        this.jobApplicationContextWorkflow = jobApplicationContextWorkflow;
    }

    @PostMapping("/job-application-context")
    @Operation(summary = "Build a bounded user-owned job application context")
    public JobApplicationContextResponse buildJobApplicationContext(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobApplicationContextRequest request
    ) {
        return JobApplicationContextResponse.from(
                jobApplicationContextWorkflow.execute(UUID.fromString(jwt.getSubject()), request)
        );
    }
}
