package com.aicareer.platform.extension.api;

import java.time.Clock;
import java.util.UUID;

import com.aicareer.platform.extension.application.ExtensionAuthorizationService;
import com.aicareer.platform.extension.application.ExtensionAnswerService;
import com.aicareer.platform.extension.application.ExtensionContextService;
import com.aicareer.platform.extension.application.ExtensionReadinessService;
import com.aicareer.platform.shared.api.PageQuery;
import com.aicareer.platform.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/extension")
@Tag(name = "Browser extension")
public class ExtensionController {

    private static final Sort VERSION_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final ExtensionAuthorizationService authorizationService;
    private final ExtensionAnswerService answerService;
    private final ExtensionContextService contextService;
    private final ExtensionReadinessService readinessService;
    private final Clock clock;

    public ExtensionController(
            ExtensionAuthorizationService authorizationService,
            ExtensionAnswerService answerService,
            ExtensionContextService contextService,
            ExtensionReadinessService readinessService,
            Clock clock
    ) {
        this.authorizationService = authorizationService;
        this.answerService = answerService;
        this.contextService = contextService;
        this.readinessService = readinessService;
        this.clock = clock;
    }

    @PostMapping("/authorization-codes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Issue a one-time browser extension authorization code")
    public ExtensionAuthorizationCodeResponse issueAuthorizationCode(@AuthenticationPrincipal Jwt jwt) {
        return ExtensionAuthorizationCodeResponse.from(
                authorizationService.issue(userId(jwt)),
                clock.instant()
        );
    }

    @PostMapping("/authorization-codes/exchange")
    @Operation(summary = "Exchange a one-time browser extension code for a short-lived access token")
    public ExtensionAuthenticationResponse exchangeAuthorizationCode(
            @Valid @RequestBody ExtensionAuthorizationCodeRequest request
    ) {
        return ExtensionAuthenticationResponse.from(authorizationService.exchange(request.code()));
    }

    @GetMapping("/resume-versions")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List the authenticated user's immutable ResumeVersion choices")
    public PageResponse<ExtensionResumeVersionResponse> listResumeVersions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                contextService.listVersions(userId(jwt), pageQuery.toPageable(VERSION_SORT)),
                ExtensionResumeVersionResponse::from
        );
    }

    @GetMapping("/resume-versions/{versionId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Read a selected ResumeVersion and its provenance metadata")
    public ExtensionContextResponse getResumeVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId
    ) {
        return ExtensionContextResponse.from(
                contextService.getVersion(userId(jwt), versionId),
                java.util.List.of()
        );
    }

    @PostMapping("/application-context")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Return selected resume context and grounded career evidence for a form question")
    public ExtensionContextResponse applicationContext(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExtensionContextRequest request
    ) {
        UUID userId = userId(jwt);
        return ExtensionContextResponse.from(
                contextService.getVersion(userId, request.resumeVersionId()),
                contextService.searchEvidence(userId, request.question())
        );
    }

    @PostMapping("/application-answer")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate a grounded answer proposal for explicit user review")
    public ExtensionAnswerResponse applicationAnswer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExtensionAnswerRequest request
    ) {
        return answerService.generate(userId(jwt), request);
    }

    @GetMapping("/session")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify a short-lived browser extension session")
    public ExtensionSessionResponse session() {
        return new ExtensionSessionResponse(true);
    }

    @GetMapping("/readiness")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Check whether career evidence is ready for extension generation")
    public ExtensionReadinessResponse readiness(@AuthenticationPrincipal Jwt jwt) {
        return readinessService.get(userId(jwt));
    }

    @PostMapping("/page-application-answer")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate a grounded proposal from the current unsaved application page")
    public ExtensionPageAnswerResponse pageApplicationAnswer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExtensionPageAnswerRequest request
    ) {
        return answerService.generateFromPage(userId(jwt), request);
    }

    @PostMapping("/page-resume-draft")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate a review-only resume draft from the current unsaved application page")
    public ExtensionPageResumeDraftResponse pageResumeDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExtensionPageResumeDraftRequest request
    ) {
        return ExtensionPageResumeDraftResponse.from(
                request,
                answerService.generateResumeDraftFromPage(userId(jwt), request)
        );
    }

    @PostMapping("/page-resume-draft/approve")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Save a reviewed current-page resume draft as a new ResumeVersion")
    public ResponseEntity<ExtensionPageResumeDraftApprovalResponse> approvePageResumeDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExtensionPageResumeDraftApprovalRequest request
    ) {
        return ResponseEntity.status(CREATED).body(
                ExtensionPageResumeDraftApprovalResponse.from(
                        answerService.approvePageResumeDraft(
                                userId(jwt),
                                request.companyName(),
                                request.jobTitle(),
                                request.draft()
                        )
                )
        );
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
