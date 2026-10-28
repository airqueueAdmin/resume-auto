package com.aicareer.platform.identity.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;

import com.aicareer.platform.identity.application.EmailVerificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ResendEmailVerificationSenderTest {

    @Test
    void springCanConstructTheProductionSender() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("emailVerificationProperties", properties());
            context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());

            assertThat(context.getBeanFactory().createBean(ResendEmailVerificationSender.class)).isNotNull();
        }
    }

    @Test
    void buildsHttpsRequestWithoutPuttingApiKeyInRequestBody() throws Exception {
        EmailVerificationProperties properties = properties();
        ResendEmailVerificationSender sender = new ResendEmailVerificationSender(
                properties,
                new ObjectMapper(),
                HttpClient.newHttpClient()
        );

        var request = sender.buildRequest("member@example.com", "123456", Duration.ofMinutes(5));

        assertThat(request.uri()).isEqualTo(java.net.URI.create("https://api.resend.com/emails"));
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer resend-api-key");
        String body = sender.buildRequestBody("member@example.com", "123456", Duration.ofMinutes(5));
        assertThat(body)
                .contains("member@example.com", "123456", "Career Note 이메일 인증 코드")
                .doesNotContain("resend-api-key");
    }

    private static EmailVerificationProperties properties() {
        return new EmailVerificationProperties(
                true,
                "resend",
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                5,
                Base64.getEncoder().encodeToString(new byte[32]),
                "https://api.resend.com/emails",
                "resend-api-key",
                "smtp.gmail.com",
                587,
                "smtp-user",
                "smtp-password",
                true,
                false,
                "noreply@example.com",
                "Career Note"
        );
    }
}
