package com.aicareer.platform.identity.application;

import java.time.Clock;

import com.aicareer.platform.identity.infrastructure.security.TokenHashing;
import com.aicareer.platform.shared.ratelimit.InMemoryRateLimiter;
import com.aicareer.platform.shared.ratelimit.RateLimitProperties;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationRateLimiter {

    private static final String GLOBAL_DELIVERY_KEY = "email-verification:delivery:global";

    private final boolean enabled;
    private final InMemoryRateLimiter clientRequestLimiter;
    private final InMemoryRateLimiter deliveryLimiter;

    public EmailVerificationRateLimiter(EmailVerificationRateLimitProperties properties, Clock clock) {
        this.enabled = properties.enabled();
        this.clientRequestLimiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, properties.clientWindow(), properties.maxClientRequests()),
                clock
        );
        this.deliveryLimiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, properties.deliveryWindow(), properties.maxDeliveries()),
                clock
        );
    }

    public void checkClientRequest(String clientAddress) {
        if (!enabled) {
            return;
        }
        String normalizedAddress = clientAddress == null || clientAddress.isBlank()
                ? "unknown"
                : clientAddress.trim();
        clientRequestLimiter.check("email-verification:client:" + TokenHashing.sha256(normalizedAddress));
    }

    public void checkDelivery() {
        if (enabled) {
            deliveryLimiter.check(GLOBAL_DELIVERY_KEY);
        }
    }
}
