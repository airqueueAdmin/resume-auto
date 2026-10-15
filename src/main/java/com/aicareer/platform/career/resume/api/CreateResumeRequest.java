package com.aicareer.platform.career.resume.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResumeRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100_000) String content
) {
}
