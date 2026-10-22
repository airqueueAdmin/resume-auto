package com.aicareer.platform.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.aicareer.platform.shared.ratelimit.RateLimitExceededException;
import org.junit.jupiter.api.Test;

class EmailVerificationRateLimiterTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-21T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void limitsEachClientIndependently() {
        EmailVerificationRateLimiter limiter = limiter(2, 10);

        limiter.checkClientRequest("192.0.2.1");
        limiter.checkClientRequest("192.0.2.1");
        limiter.checkClientRequest("192.0.2.2");

        assertThatThrownBy(() -> limiter.checkClientRequest("192.0.2.1"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void capsActualDeliveriesAcrossAllClients() {
        EmailVerificationRateLimiter limiter = limiter(10, 2);

        limiter.checkDelivery();
        limiter.checkDelivery();

        assertThatThrownBy(limiter::checkDelivery)
                .isInstanceOf(RateLimitExceededException.class);
    }

    private EmailVerificationRateLimiter limiter(int maxClientRequests, int maxDeliveries) {
        return new EmailVerificationRateLimiter(
                new EmailVerificationRateLimitProperties(
                        true,
                        Duration.ofHours(1),
                        maxClientRequests,
                        Duration.ofDays(1),
                        maxDeliveries
                ),
                CLOCK
        );
    }
}
