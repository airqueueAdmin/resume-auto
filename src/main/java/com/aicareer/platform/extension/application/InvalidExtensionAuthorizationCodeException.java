package com.aicareer.platform.extension.application;

public class InvalidExtensionAuthorizationCodeException extends RuntimeException {

    public InvalidExtensionAuthorizationCodeException() {
        super("Extension authorization code is invalid or expired");
    }
}
