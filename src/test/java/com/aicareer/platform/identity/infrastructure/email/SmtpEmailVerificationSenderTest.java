package com.aicareer.platform.identity.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Base64;

import com.aicareer.platform.identity.application.EmailVerificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class SmtpEmailVerificationSenderTest {

    @Test
    void configuresGmailSubmissionWithRequiredStartTls() {
        EmailVerificationProperties properties = new EmailVerificationProperties(
                true,
                "smtp",
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                5,
                Base64.getEncoder().encodeToString(new byte[32]),
                "https://api.resend.com/emails",
                "resend-api-key",
                "smtp.gmail.com",
                587,
                "sender@example.com",
                "app-password",
                true,
                false,
                "sender@example.com",
                "Career Note"
        );

        JavaMailSenderImpl sender = SmtpEmailVerificationSender.createMailSender(properties);

        assertThat(sender.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.enable", "true")
                .containsEntry("mail.smtp.starttls.required", "true")
                .containsEntry("mail.smtp.ssl.enable", "false");
    }
}
