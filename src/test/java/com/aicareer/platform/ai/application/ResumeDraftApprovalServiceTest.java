package com.aicareer.platform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.ai.domain.ResumeDraft;
import com.aicareer.platform.ai.infrastructure.persistence.ResumeDraftRepository;
import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.api.CreateResumeRequest;
import com.aicareer.platform.career.resume.application.ResumeService;
import com.aicareer.platform.career.resume.application.ResumeVersionService;
import com.aicareer.platform.career.resume.domain.Resume;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.retrieval.domain.SourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResumeDraftApprovalServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DRAFT_ID = UUID.randomUUID();
    private static final UUID RESUME_ID = UUID.randomUUID();

    @Test
    void formatsStoredGroundedDraftAndCreatesNewVersionWithoutCitationLabels() {
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        ResumeService resumeService = org.mockito.Mockito.mock(ResumeService.class);
        ResumeVersionService versionService = org.mockito.Mockito.mock(ResumeVersionService.class);
        ResumeVersion version = org.mockito.Mockito.mock(ResumeVersion.class);
        ResumeDraft draft = draft(true);
        when(repository.findByIdAndUser_Id(DRAFT_ID, USER_ID)).thenReturn(java.util.Optional.of(draft));
        when(versionService.createFromContent(any(), any(), any())).thenReturn(version);

        ResumeVersion result = service(repository, resumeService, versionService).approve(USER_ID, DRAFT_ID, RESUME_ID);

        assertThat(result).isSameAs(version);
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(versionService).createFromContent(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(RESUME_ID),
                content.capture()
        );
        assertThat(content.getValue())
                .contains("희망직무: Backend Developer")
                .contains("자기소개서:\n요약: Built production APIs")
                .contains("[자기소개서 1] 문제 해결 방식")
                .contains("[자기소개서 2] 협업과 책임감")
                .contains("[자기소개서 3] 직무 기여 방향")
                .contains("경력기술서:\n- Designed Java services\n- Career platform")
                .contains("스킬:\n- Java")
                .doesNotContain("[C1]", "[C2]");
    }

    @Test
    void approvesGroundedDraftIntoNewResumeVersionOne() {
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        ResumeService resumeService = org.mockito.Mockito.mock(ResumeService.class);
        ResumeVersionService versionService = org.mockito.Mockito.mock(ResumeVersionService.class);
        Resume resume = org.mockito.Mockito.mock(Resume.class);
        ResumeDraft draft = draft(true);
        when(repository.findByIdAndUser_Id(DRAFT_ID, USER_ID)).thenReturn(java.util.Optional.of(draft));
        when(resumeService.create(any(), any(CreateResumeRequest.class))).thenReturn(resume);

        Resume result = service(repository, resumeService, versionService)
                .approveAsNewResume(USER_ID, DRAFT_ID, "Example Corp - Backend 이력서");

        assertThat(result).isSameAs(resume);
        ArgumentCaptor<CreateResumeRequest> request = ArgumentCaptor.forClass(CreateResumeRequest.class);
        verify(resumeService).create(org.mockito.ArgumentMatchers.eq(USER_ID), request.capture());
        assertThat(request.getValue().name()).isEqualTo("Example Corp - Backend 이력서");
        assertThat(request.getValue().content())
                .contains("희망직무: Backend Developer")
                .contains("[자기소개서 3] 직무 기여 방향")
                .doesNotContain("[C1]", "[C2]");
        verify(versionService, never()).createFromContent(any(), any(), any());
    }

    @Test
    void doesNotApproveDraftOwnedByAnotherUser() {
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        ResumeService resumeService = org.mockito.Mockito.mock(ResumeService.class);
        ResumeVersionService versionService = org.mockito.Mockito.mock(ResumeVersionService.class);
        when(repository.findByIdAndUser_Id(DRAFT_ID, USER_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service(repository, resumeService, versionService)
                .approve(USER_ID, DRAFT_ID, RESUME_ID))
                .isInstanceOf(CareerResourceNotFoundException.class);
        verify(versionService, never()).createFromContent(any(), any(), any());
    }

    @Test
    void rejectsStoredLineWithoutCitation() {
        ResumeDraftRepository repository = org.mockito.Mockito.mock(ResumeDraftRepository.class);
        ResumeService resumeService = org.mockito.Mockito.mock(ResumeService.class);
        ResumeVersionService versionService = org.mockito.Mockito.mock(ResumeVersionService.class);
        when(repository.findByIdAndUser_Id(DRAFT_ID, USER_ID)).thenReturn(java.util.Optional.of(draft(false)));

        assertThatThrownBy(() -> service(repository, resumeService, versionService)
                .approve(USER_ID, DRAFT_ID, RESUME_ID))
                .isInstanceOf(GroundingValidationException.class)
                .extracting(exception -> ((GroundingValidationException) exception).kind())
                .isEqualTo(GroundingValidationException.Kind.UNCITED_FINDING);
        verify(versionService, never()).createFromContent(any(), any(), any());
    }

    private ResumeDraftApprovalService service(
            ResumeDraftRepository repository,
            ResumeService resumeService,
            ResumeVersionService versionService
    ) {
        return new ResumeDraftApprovalService(
                repository,
                resumeService,
                versionService,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private ResumeDraft draft(boolean cited) {
        User user = User.create("draft-owner@example.com", "hash", "Draft Owner", Instant.now());
        JobPosting jobPosting = JobPosting.create(
                user,
                "Backend Developer",
                "Example Corp",
                "Build Java APIs.",
                null,
                Instant.now()
        );
        SourceCitation c1 = citation("C1");
        SourceCitation c2 = citation("C2");
        GroundedResumeDraftLine headline = line("Backend Developer [C1] [c2]", c1);
        GroundedResumeDraftLine summary = line("Built production APIs", c1);
        List<GroundedResumeDraftSection> selfIntroductionSections = List.of(
                section("문제 해결 방식", "복잡한 문제를 작은 단위로 나누고 핵심 원인을 먼저 확인했습니다. 구현 전 검증 기준을 세워 불필요한 재작업을 줄였고, 결과적으로 운영 가능한 API를 완성했습니다.", c1),
                section("협업과 책임감", "팀원이 이어서 작업할 수 있도록 API 계약과 변경 이유를 명확히 정리했습니다. 구현 결과를 공유하고 피드백을 반영하며 개인의 작업을 팀의 성과로 연결했습니다.", c2),
                section("직무 기여 방향", "Java 서비스와 프로젝트를 구현한 경험을 바탕으로 안정적인 백엔드 기능을 만들겠습니다. 검증 가능한 결과와 지속적인 개선을 통해 지원 직무의 서비스 품질 향상에 기여하겠습니다.", c1, c2)
        );
        GroundedResumeDraftLine experience = cited
                ? line("Designed Java services", c1)
                : new GroundedResumeDraftLine("Designed Java services", List.of());
        GroundedResumeDraftDocument document = new GroundedResumeDraftDocument(
                headline,
                summary,
                selfIntroductionSections,
                List.of(experience),
                List.of(line("Career platform", c2)),
                List.of(new GroundedResumeDraftLine("Java", List.of(c1, c2)))
        );
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return ResumeDraft.create(
                user,
                jobPosting,
                "ollama",
                "llama3.1:8b",
                "resume-draft-v1",
                "provider-response",
                mapper.valueToTree(document),
                mapper.valueToTree(List.of(c1, c2)),
                2,
                new LlmUsage(10, 20, 30),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private GroundedResumeDraftLine line(String text, SourceCitation citation) {
        return new GroundedResumeDraftLine(text, List.of(citation));
    }

    private GroundedResumeDraftSection section(
            String title,
            String content,
            SourceCitation... citations
    ) {
        return new GroundedResumeDraftSection(title, content, List.of(citations));
    }

    private SourceCitation citation(String id) {
        return new SourceCitation(
                id,
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.randomUUID(),
                0,
                "Source content",
                0.91,
                "nomic-embed-text"
        );
    }
}
