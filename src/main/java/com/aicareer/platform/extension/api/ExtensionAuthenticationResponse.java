package com.aicareer.platform.extension.api;

import com.aicareer.platform.extension.application.ExtensionAuthenticationResult;

public record ExtensionAuthenticationResponse(
        UserSummary user,
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds
) {

    public static ExtensionAuthenticationResponse from(ExtensionAuthenticationResult result) {
        return new ExtensionAuthenticationResponse(
                UserSummary.from(result.user()),
                "Bearer",
                result.accessToken(),
                result.accessTokenTtl().toSeconds()
        );
    }

    public record UserSummary(String id, String email, String displayName) {

        public static UserSummary from(com.aicareer.platform.identity.domain.User user) {
            return new UserSummary(user.getId().toString(), user.getEmail(), user.getDisplayName());
        }
    }
}
