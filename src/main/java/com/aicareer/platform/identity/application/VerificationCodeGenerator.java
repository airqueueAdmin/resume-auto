package com.aicareer.platform.identity.application;

@FunctionalInterface
public interface VerificationCodeGenerator {

    String generate();
}
