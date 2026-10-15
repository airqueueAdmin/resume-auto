package com.aicareer.platform.identity.application;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User was not found");
    }
}
