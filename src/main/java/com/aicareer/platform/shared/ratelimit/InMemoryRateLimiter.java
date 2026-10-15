package com.aicareer.platform.shared.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryRateLimiter {

    private final RateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(RateLimitProperties properties, Clock clock) {
        if (properties == null || clock == null) {
            throw new IllegalArgumentException("Rate-limit properties and clock are required");
        }
        this.properties = properties;
        this.clock = clock;
    }

    public void check(String key) {
        if (!properties.enabled()) {
            return;
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate-limit key must not be blank");
        }

        Instant now = clock.instant();
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));
        synchronized (counter) {
            if (counters.get(key) != counter) {
                check(key);
                return;
            }
            if (!now.isBefore(counter.windowStarted().plus(properties.window()))) {
                counter.reset(now);
            }
            if (counter.count() >= properties.maxRequests()) {
                Duration remaining = Duration.between(now, counter.windowStarted().plus(properties.window()));
                throw new RateLimitExceededException(Math.max(1, (remaining.toMillis() + 999) / 1000));
            }
            counter.increment();
        }
    }

    int trackedKeyCount() {
        return counters.size();
    }

    private static final class WindowCounter {
        private Instant windowStarted;
        private int count;

        private WindowCounter(Instant windowStarted) {
            this.windowStarted = windowStarted;
        }

        private Instant windowStarted() {
            return windowStarted;
        }

        private int count() {
            return count;
        }

        private void increment() {
            count++;
        }

        private void reset(Instant started) {
            windowStarted = started;
            count = 0;
        }
    }
}
