package com.aicareer.platform.ai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveResumeDraftAsNewRequest(
        @NotBlank @Size(max = 200) String resumeName
) {
}
