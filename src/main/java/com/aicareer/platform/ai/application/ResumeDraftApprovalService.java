package com.aicareer.platform.ai.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.aicareer.platform.ai.domain.ResumeDraft;
import com.aicareer.platform.ai.infrastructure.persistence.ResumeDraftRepository;
import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.api.CreateResumeRequest;
import com.aicareer.platform.career.resume.application.ResumeService;
import com.aicareer.platform.career.resume.application.ResumeVersionService;
import com.aicareer.platform.career.resume.domain.Resume;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeDraftApprovalService {

    private static final int MAX_RESUME_CONTENT_CHARACTERS = 100_000;

    private final ResumeDraftRepository resumeDraftRepository;
    private final ResumeService resumeService;
    private final ResumeVersionService resumeVersionService;
    private final ObjectMapper objectMapper;

    public ResumeDraftApprovalService(
            ResumeDraftRepository resumeDraftRepository,
            ResumeService resumeService,
            ResumeVersionService resumeVersionService,
            ObjectMapper objectMapper
    ) {
        this.resumeDraftRepository = resumeDraftRepository;
        this.resumeService = resumeService;
        this.resumeVersionService = resumeVersionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResumeVersion approve(UUID userId, UUID draftId, UUID resumeId) {
        ResumeDraft draft = resumeDraftRepository.findByIdAndUser_Id(draftId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
        String content = format(parseDraft(draft.getDraft()));
        return resumeVersionService.createFromContent(userId, resumeId, content);
    }

    @Transactional
    public Resume approveAsNewResume(UUID userId, UUID draftId, String resumeName) {
        ResumeDraft draft = resumeDraftRepository.findByIdAndUser_Id(draftId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
        String content = format(parseDraft(draft.getDraft()));
        return resumeService.create(userId, new CreateResumeRequest(resumeName, content));
    }

    private GroundedResumeDraftDocument parseDraft(JsonNode draftJson) {
        if (draftJson == null || draftJson.isNull()) {
            throw GroundingValidationException.invalidFinding();
        }
        try {
            return objectMapper.treeToValue(draftJson, GroundedResumeDraftDocument.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw GroundingValidationException.invalidFinding();
        }
    }

    private String format(GroundedResumeDraftDocument draft) {
        StringBuilder content = new StringBuilder();
        appendLine(content, "희망직무", draft.headline(), false);
        if (draft.selfIntroductionSections() == null || draft.selfIntroductionSections().isEmpty()) {
            // Keep approval compatible with older immutable draft artifacts.
            appendLine(content, "자기소개서", draft.summary(), false);
        } else {
            appendSelfIntroduction(content, draft.summary(), draft.selfIntroductionSections());
        }
        appendSection(content, "경력기술서", Stream.concat(
                draft.experienceBullets().stream(),
                draft.projectBullets().stream()
        ).toList());
        appendSection(content, "스킬", draft.skills());

        String formatted = content.toString().strip();
        if (formatted.isBlank() || formatted.length() > MAX_RESUME_CONTENT_CHARACTERS) {
            throw GroundingValidationException.invalidFinding();
        }
        return formatted;
    }

    private void appendSelfIntroduction(
            StringBuilder content,
            GroundedResumeDraftLine summary,
            List<GroundedResumeDraftSection> sections
    ) {
        content.append("자기소개서:\n");
        appendLine(content, "요약", summary, false);
        for (int index = 0; index < sections.size(); index++) {
            GroundedResumeDraftSection section = sections.get(index);
            if (section == null || section.title() == null || section.title().isBlank()
                    || section.content() == null || section.content().isBlank()) {
                throw GroundingValidationException.invalidFinding();
            }
            List<String> citationIds = citationIds(section.citations());
            if (citationIds.isEmpty()) {
                throw GroundingValidationException.uncitedFinding();
            }
            appendCitedText(
                    content,
                    "[자기소개서 " + (index + 1) + "] " + section.title().strip(),
                    citationIds
            );
            appendCitedText(content, section.content().strip(), citationIds);
        }
    }

    private void appendSection(
            StringBuilder content,
            String section,
            List<GroundedResumeDraftLine> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int before = content.length();
        for (GroundedResumeDraftLine line : lines) {
            appendLine(content, section, line, true);
        }
        if (content.length() > before) {
            content.insert(before, section + ":\n");
        }
    }

    private void appendLine(
            StringBuilder content,
            String label,
            GroundedResumeDraftLine line,
            boolean bullet
    ) {
        if (line == null || line.text() == null || line.text().isBlank()) {
            return;
        }
        List<String> citationIds = citationIds(line.citations());
        if (citationIds.isEmpty()) {
            throw GroundingValidationException.uncitedFinding();
        }
        String prefix = bullet ? "- " : label + ": ";
        content.append(prefix);
        appendCitedText(content, line.text().strip(), citationIds);
    }

    private List<String> citationIds(List<SourceCitation> citations) {
        return citations == null
                ? List.of()
                : citations.stream()
                        .filter(Objects::nonNull)
                        .map(SourceCitation::citationId)
                        .filter(id -> id != null && !id.isBlank())
                        .distinct()
                        .toList();
    }

    private void appendCitedText(StringBuilder content, String text, List<String> citationIds) {
        content.append(text)
                .append(" [")
                .append(citationIds.stream().collect(Collectors.joining(", ")))
                .append("]\n");
    }
}
