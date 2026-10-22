package com.aicareer.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import com.aicareer.platform.identity.application.EmailVerificationMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(EmailVerificationApiTests.EmailCaptureConfiguration.class)
class EmailVerificationApiTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("email_verification_test")
            .withUsername("email_verification_test")
            .withPassword("email_verification_test");

    @LocalServerPort
    private int port;

    @Autowired
    private CapturingEmailSender emailSender;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void emailVerificationProperties(DynamicPropertyRegistry properties) {
        properties.add("app.identity.email-verification.enabled", () -> true);
        properties.add("app.identity.email-verification.smtp-host", () -> "smtp.test.invalid");
        properties.add("app.identity.email-verification.smtp-username", () -> "test-user");
        properties.add("app.identity.email-verification.smtp-password", () -> "test-password");
        properties.add("app.identity.email-verification.from-address", () -> "noreply@example.com");
        properties.add("app.identity.email-verification.rate-limit.enabled", () -> false);
    }

    @BeforeEach
    void resetSender() {
        emailSender.reset();
    }

    @Test
    void pendingAccountCannotLoginUntilTheDeliveredCodeIsVerified() throws Exception {
        String email = "verified-user@example.com";
        String password = "correct-password-123";

        HttpResponse<String> signup = post("/api/v1/auth/signup", """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "Verified User",
                  "termsAccepted": true,
                  "privacyAccepted": true
                }
                """.formatted(email, password));

        assertThat(signup.statusCode()).isEqualTo(202);
        assertThat(signup.body()).contains("\"verificationRequired\":true");
        assertThat(emailSender.recipient()).isEqualTo(email);
        assertThat(emailSender.code()).matches("\\d{6}");

        HttpResponse<String> loginBeforeVerification = login(email, password);
        assertThat(loginBeforeVerification.statusCode()).isEqualTo(403);
        assertThat(loginBeforeVerification.body()).contains("EMAIL_VERIFICATION_REQUIRED");

        HttpResponse<String> wrongCode = post("/api/v1/auth/verify-email", """
                {"email":"%s","code":"000000"}
                """.formatted(email));
        assertThat(wrongCode.statusCode()).isEqualTo(400);
        assertThat(wrongCode.body()).contains("EMAIL_VERIFICATION_INVALID");

        HttpResponse<String> verification = post("/api/v1/auth/verify-email", """
                {"email":"%s","code":"%s"}
                """.formatted(email, emailSender.code()));
        assertThat(verification.statusCode()).isEqualTo(204);

        HttpResponse<String> loginAfterVerification = login(email, password);
        assertThat(loginAfterVerification.statusCode()).isEqualTo(200);
        assertThat(loginAfterVerification.body()).contains("\"tokenType\":\"Bearer\"");
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return post("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(email, password));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmailCaptureConfiguration {

        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    static final class CapturingEmailSender implements EmailVerificationMessageSender {

        private final AtomicReference<String> recipient = new AtomicReference<>();
        private final AtomicReference<String> code = new AtomicReference<>();

        @Override
        public void sendVerificationCode(String recipient, String code, Duration validFor) {
            this.recipient.set(recipient);
            this.code.set(code);
        }

        String recipient() {
            return recipient.get();
        }

        String code() {
            return code.get();
        }

        void reset() {
            recipient.set(null);
            code.set(null);
        }
    }
}
