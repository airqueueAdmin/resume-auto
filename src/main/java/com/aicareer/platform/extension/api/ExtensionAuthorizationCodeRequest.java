package com.aicareer.platform.extension.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtensionAuthorizationCodeRequest(
        @NotBlank @Size(max = 200) String code
) {
}
