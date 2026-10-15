package com.aicareer.platform.career.resume.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameResumeRequest(
        @NotBlank @Size(max = 200) String name
) {
}
