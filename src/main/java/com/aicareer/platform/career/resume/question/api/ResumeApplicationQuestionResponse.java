package com.aicareer.platform.career.resume.question.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.resume.question.domain.ResumeApplicationQuestion;

public record ResumeApplicationQuestionResponse(
        UUID id,
        UUID resumeId,
        UUID jobPostingId,
        String question,
        String answer,
        Instant createdAt,
        Instant updatedAt
) {

    public static ResumeApplicationQuestionResponse from(ResumeApplicationQuestion item) {
        return new ResumeApplicationQuestionResponse(
                item.getId(),
                item.getResumeId(),
                item.getJobPostingId(),
                item.getQuestion(),
                item.getAnswer(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
