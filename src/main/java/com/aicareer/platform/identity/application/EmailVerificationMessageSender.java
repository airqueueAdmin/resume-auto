package com.aicareer.platform.identity.application;

import java.time.Duration;

public interface EmailVerificationMessageSender {

    void sendVerificationCode(String recipient, String code, Duration validFor);
}
