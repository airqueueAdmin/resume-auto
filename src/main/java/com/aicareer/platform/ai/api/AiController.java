package com.aicareer.platform.ai.api;

import java.util.UUID;

import com.aicareer.platform.ai.application.JobAnalysisOutcome;
import com.aicareer.platform.ai.application.JobAnalysisService;
import com.aicareer.platform.ai.application.JobMatchOutcome;
import com.aicareer.platform.ai.application.RagMatchingService;
import com.aicareer.platform.ai.application.ResumeDraftApprovalService;
import com.aicareer.platform.ai.application.ResumeDraftOutcome;
import com.aicareer.platform.ai.application.ResumeDraftService;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerOutcome;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerService;
import com.aicareer.platform.career.resume.api.ResumeResponse;
import com.aicareer.platform.career.resume.api.ResumeVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final JobAnalysisService jobAnalysisService;
    private final RagMatchingService ragMatchingService;
    private final ResumeDraftService resumeDraftService;
    private final ResumeDraftApprovalService resumeDraftApprovalService;
    private final SelfIntroductionAnswerService selfIntroductionAnswerService;
    private final Environment environment;

    public AiController(
            JobAnalysisService jobAnalysisService,
            RagMatchingService ragMatchingService,
            ResumeDraftService resumeDraftService,
            ResumeDraftApprovalService resumeDraftApprovalService,
            SelfIntroductionAnswerService selfIntroductionAnswerService,
            Environment environment
    ) {
        this.jobAnalysisService = jobAnalysisService;
        this.ragMatchingService = ragMatchingService;
        this.resumeDraftService = resumeDraftService;
        this.resumeDraftApprovalService = resumeDraftApprovalService;
        this.selfIntroductionAnswerService = selfIntroductionAnswerService;
        this.environment = environment;
    }

    @PostMapping("/analyze-job")
    @Operation(summary = "Analyze a saved job posting with structured LLM output")
    public JobAnalysisResponse analyzeJob(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalyzeJobRequest request
    ) {
        JobAnalysisOutcome outcome = jobAnalysisService.analyze(
                UUID.fromString(jwt.getSubject()),
                request.jobPostingId()
        );
        return JobAnalysisResponse.from(outcome);
    }

    @PostMapping("/match-job")
    @Operation(summary = "Match a saved job posting against retrieved career evidence")
    public JobMatchResponse matchJob(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalyzeJobRequest request
    ) {
        JobMatchOutcome outcome = ragMatchingService.match(
                UUID.fromString(jwt.getSubject()),
                request.jobPostingId()
        );
        return JobMatchResponse.from(outcome);
    }

    @PostMapping("/generate-resume-draft")
    @Operation(summary = "Generate a grounded resume draft from retrieved career evidence")
    public ResumeDraftResponse generateResumeDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateResumeDraftRequest request
    ) {
        ResumeDraftOutcome outcome = resumeDraftService.generate(
                UUID.fromString(jwt.getSubject()),
                request.jobPostingId()
        );
        return ResumeDraftResponse.from(outcome);
    }

    @PostMapping("/generate-self-introduction-answer")
    @Operation(summary = "Generate a grounded answer for a saved question and its linked job posting")
    public SelfIntroductionAnswerResponse generateSelfIntroductionAnswer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateSelfIntroductionAnswerRequest request
    ) {
        SelfIntroductionAnswerOutcome outcome = selfIntroductionAnswerService.generate(
                UUID.fromString(jwt.getSubject()),
                request.resumeId(),
                request.questionId()
        );
        return SelfIntroductionAnswerResponse.from(outcome, environment.acceptsProfiles(Profiles.of("local")));
    }

    @PostMapping("/resume-drafts/{draftId}/approve")
    @Operation(summary = "Approve a grounded resume draft into a new immutable ResumeVersion")
    public ResponseEntity<ResumeVersionResponse> approveResumeDraft(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID draftId,
            @Valid @RequestBody ApproveResumeDraftRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResumeVersionResponse.from(resumeDraftApprovalService.approve(
                        UUID.fromString(jwt.getSubject()),
                        draftId,
                        request.resumeId()
                )));
    }

    @PostMapping("/resume-drafts/{draftId}/approve-new")
    @Operation(summary = "Approve a grounded resume draft into a new Resume and immutable Version 1")
    public ResponseEntity<ResumeResponse> approveResumeDraftAsNew(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID draftId,
            @Valid @RequestBody ApproveResumeDraftAsNewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResumeResponse.from(resumeDraftApprovalService.approveAsNewResume(
                        UUID.fromString(jwt.getSubject()),
                        draftId,
                        request.resumeName()
                )));
    }
}
