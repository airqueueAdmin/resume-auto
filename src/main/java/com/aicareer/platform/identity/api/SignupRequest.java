package com.aicareer.platform.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(min = 2, max = 100) String displayName,
        @NotNull @AssertTrue(message = "terms must be accepted") Boolean termsAccepted,
        @NotNull @AssertTrue(message = "privacy policy must be accepted") Boolean privacyAccepted
) {
}
