package com.aicareer.platform.career.resume.question.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResumeApplicationQuestionRequest(
        @NotNull UUID jobPostingId,
        @NotBlank @Size(max = 2000) String question,
        @Size(max = 100_000) String answer
) {
}
