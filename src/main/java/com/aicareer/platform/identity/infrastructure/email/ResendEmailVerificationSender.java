package com.aicareer.platform.identity.infrastructure.email;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.aicareer.platform.identity.application.EmailVerificationException;
import com.aicareer.platform.identity.application.EmailVerificationMessageSender;
import com.aicareer.platform.identity.application.EmailVerificationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.identity.email-verification",
        name = "provider",
        havingValue = "resend"
)
public class ResendEmailVerificationSender implements EmailVerificationMessageSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailVerificationSender.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final EmailVerificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailVerificationSender(
            EmailVerificationProperties properties,
            ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build());
    }

    ResendEmailVerificationSender(
            EmailVerificationProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public void sendVerificationCode(String recipient, String code, Duration validFor) {
        if (!properties.enabled()) {
            throw new IllegalStateException("Email delivery is disabled");
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    buildRequest(recipient, code, validFor),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Resend email delivery failed status={}", response.statusCode());
                throw deliveryUnavailable();
            }
        } catch (IOException exception) {
            throw new EmailVerificationException(
                    EmailVerificationException.Kind.DELIVERY_UNAVAILABLE,
                    "The verification email could not be delivered",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EmailVerificationException(
                    EmailVerificationException.Kind.DELIVERY_UNAVAILABLE,
                    "The verification email could not be delivered",
                    exception
            );
        }
    }

    HttpRequest buildRequest(String recipient, String code, Duration validFor)
            throws JsonProcessingException {
        String body = buildRequestBody(recipient, code, validFor);
        return HttpRequest.newBuilder(URI.create(properties.resendApiUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + properties.resendApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    String buildRequestBody(String recipient, String code, Duration validFor)
            throws JsonProcessingException {
        long validMinutes = Math.max(1, validFor.toMinutes());
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;color:#27342a">
                  <p style="font-size:12px;color:#66805f">CAREER NOTE</p>
                  <h1 style="font-size:24px">이메일을 확인해주세요</h1>
                  <p>회원가입을 계속하려면 아래 인증코드를 입력해주세요.</p>
                  <p style="margin:28px 0;font-size:34px;font-weight:700;letter-spacing:8px;color:#496b3e">%s</p>
                  <p style="font-size:13px;color:#6f7b71">이 코드는 %d분 동안 유효합니다. 직접 요청하지 않았다면 이 메일을 무시해주세요.</p>
                </div>
                """.formatted(code, validMinutes);
        String from = properties.fromName().isBlank()
                ? properties.fromAddress()
                : "%s <%s>".formatted(properties.fromName(), properties.fromAddress());
        String body = objectMapper.writeValueAsString(new ResendEmailRequest(
                from,
                new String[]{recipient},
                "Career Note 이메일 인증 코드",
                html
        ));
        return body;
    }

    private EmailVerificationException deliveryUnavailable() {
        return new EmailVerificationException(
                EmailVerificationException.Kind.DELIVERY_UNAVAILABLE,
                "The verification email could not be delivered"
        );
    }

    private record ResendEmailRequest(
            String from,
            String[] to,
            String subject,
            String html
    ) {
    }
}
