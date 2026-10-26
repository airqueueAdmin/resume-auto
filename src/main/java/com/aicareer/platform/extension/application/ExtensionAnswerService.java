package com.aicareer.platform.extension.application;

import java.util.UUID;

import com.aicareer.platform.ai.application.GroundedResumeDraftDocument;
import com.aicareer.platform.ai.application.PageResumeDraftOutcome;
import com.aicareer.platform.ai.application.ResumeDraftApprovalService;
import com.aicareer.platform.ai.application.ResumeDraftService;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerOutcome;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerService;
import com.aicareer.platform.ai.application.SelfIntroductionJobContext;
import com.aicareer.platform.career.resume.domain.Resume;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.extension.api.ExtensionAnswerRequest;
import com.aicareer.platform.extension.api.ExtensionAnswerResponse;
import com.aicareer.platform.extension.api.ExtensionPageAnswerRequest;
import com.aicareer.platform.extension.api.ExtensionPageAnswerResponse;
import com.aicareer.platform.extension.api.ExtensionPageResumeDraftRequest;
import org.springframework.stereotype.Service;

@Service
public class ExtensionAnswerService {

    private final ExtensionContextService contextService;
    private final SelfIntroductionAnswerService selfIntroductionAnswerService;
    private final ResumeDraftService resumeDraftService;
    private final ResumeDraftApprovalService resumeDraftApprovalService;

    public ExtensionAnswerService(
            ExtensionContextService contextService,
            SelfIntroductionAnswerService selfIntroductionAnswerService,
            ResumeDraftService resumeDraftService,
            ResumeDraftApprovalService resumeDraftApprovalService
    ) {
        this.contextService = contextService;
        this.selfIntroductionAnswerService = selfIntroductionAnswerService;
        this.resumeDraftService = resumeDraftService;
        this.resumeDraftApprovalService = resumeDraftApprovalService;
    }

    public ExtensionAnswerResponse generate(UUID userId, ExtensionAnswerRequest request) {
        ResumeVersion version = contextService.getVersion(userId, request.resumeVersionId());
        SelfIntroductionAnswerOutcome outcome = selfIntroductionAnswerService.generateForQuestion(
                userId,
                request.jobPostingId(),
                request.question().strip()
        );
        String answer = outcome.answer();
        String suggestedValue = request.maxLength() == null
                ? answer
                : answer.substring(0, Math.min(answer.length(), request.maxLength()));
        return ExtensionAnswerResponse.from(
                version,
                request.jobPostingId(),
                request.maxLength(),
                suggestedValue,
                outcome
        );
    }

    public ExtensionPageAnswerResponse generateFromPage(UUID userId, ExtensionPageAnswerRequest request) {
        SelfIntroductionAnswerOutcome outcome = selfIntroductionAnswerService.generateForPagePosting(
                userId,
                new SelfIntroductionJobContext(
                        request.companyName().strip(),
                        request.jobTitle().strip(),
                        request.jobDescription().strip()
                ),
                request.question().strip()
        );
        String answer = outcome.answer();
        String suggestedValue = request.maxLength() == null
                ? answer
                : answer.substring(0, Math.min(answer.length(), request.maxLength()));
        return ExtensionPageAnswerResponse.from(request, suggestedValue, outcome);
    }

    public PageResumeDraftOutcome generateResumeDraftFromPage(
            UUID userId,
            ExtensionPageResumeDraftRequest request
    ) {
        return resumeDraftService.generateForPage(
                userId,
                request.companyName(),
                request.jobTitle(),
                request.jobDescription(),
                request.sourceUrl()
        );
    }

    public Resume approvePageResumeDraft(
            UUID userId,
            String companyName,
            String jobTitle,
            GroundedResumeDraftDocument draft
    ) {
        String resumeName = (companyName.strip() + " - " + jobTitle.strip() + " 이력서")
                .strip();
        resumeName = resumeName.substring(0, Math.min(200, resumeName.length()));
        return resumeDraftApprovalService.approvePageAsNewResume(userId, resumeName, draft);
    }
}
