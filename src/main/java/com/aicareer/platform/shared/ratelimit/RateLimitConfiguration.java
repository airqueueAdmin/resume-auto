package com.aicareer.platform.shared.ratelimit;

import java.time.Clock;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration implements WebMvcConfigurer {

    private final InMemoryRateLimiter limiter;

    public RateLimitConfiguration(RateLimitProperties properties, Clock clock) {
        this.limiter = new InMemoryRateLimiter(properties, clock);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    Object handler
            ) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String subject = authentication != null && authentication.isAuthenticated()
                        && authentication.getName() != null && !authentication.getName().isBlank()
                        && !"anonymousUser".equals(authentication.getName())
                        ? "user:" + authentication.getName()
                        : "ip:" + request.getRemoteAddr();
                limiter.check(subject + ":" + bucket(request));
                return true;
            }
        }).addPathPatterns(
                "/api/v1/ai/**",
                "/api/v1/retrieval/**",
                "/api/v1/workflows/**",
                "/api/v1/jobkorea/**",
                "/api/v1/tools/calls",
                "/api/v1/tools/approvals",
                "/api/v1/auth/**"
        );
    }

    private String bucket(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/ai/")) {
            return "ai";
        }
        if (uri.startsWith("/api/v1/retrieval/")) {
            return "retrieval";
        }
        if (uri.startsWith("/api/v1/workflows/")) {
            return "workflow";
        }
        if (uri.startsWith("/api/v1/auth/")) {
            return "auth";
        }
        if (uri.startsWith("/api/v1/jobkorea/")) {
            return "jobkorea-export";
        }
        return "tools";
    }
}
