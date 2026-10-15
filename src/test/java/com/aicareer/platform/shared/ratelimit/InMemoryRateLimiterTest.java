package com.aicareer.platform.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void rejectsRequestsAfterConfiguredWindowLimit() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, Duration.ofSeconds(10), 2),
                Clock.fixed(START, ZoneOffset.UTC)
        );

        limiter.check("user:ai");
        limiter.check("user:ai");

        assertThatThrownBy(() -> limiter.check("user:ai"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(exception -> assertThat(((RateLimitExceededException) exception).retryAfterSeconds())
                        .isEqualTo(10));
    }

    @Test
    void isolatesBucketsAndDisabledLimiterDoesNotTrackRequests() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, Duration.ofMinutes(1), 1),
                Clock.fixed(START, ZoneOffset.UTC)
        );

        limiter.check("user-a:ai");
        limiter.check("user-b:ai");

        assertThat(limiter.trackedKeyCount()).isEqualTo(2);
        assertThatThrownBy(() -> limiter.check("user-a:ai"))
                .isInstanceOf(RateLimitExceededException.class);

        InMemoryRateLimiter disabled = new InMemoryRateLimiter(
                new RateLimitProperties(false, Duration.ofMinutes(1), 1),
                Clock.fixed(START, ZoneOffset.UTC)
        );
        disabled.check("user:ai");
        disabled.check("user:ai");
        assertThat(disabled.trackedKeyCount()).isZero();
    }

    @Test
    void authAndUserBucketsCanUseDifferentSubjects() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, Duration.ofMinutes(1), 1),
                Clock.fixed(START, ZoneOffset.UTC)
        );

        limiter.check("ip:127.0.0.1:auth");
        limiter.check("user:user-id:ai");

        assertThat(limiter.trackedKeyCount()).isEqualTo(2);
    }
}
