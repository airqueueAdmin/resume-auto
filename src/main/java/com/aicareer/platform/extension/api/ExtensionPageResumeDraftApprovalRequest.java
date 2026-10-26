package com.aicareer.platform.extension.api;

import com.aicareer.platform.ai.application.GroundedResumeDraftDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExtensionPageResumeDraftApprovalRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String jobTitle,
        @NotNull @Valid GroundedResumeDraftDocument draft
) {
}
