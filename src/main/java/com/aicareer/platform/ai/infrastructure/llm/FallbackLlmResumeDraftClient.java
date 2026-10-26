package com.aicareer.platform.ai.infrastructure.llm;

import java.util.List;

import com.aicareer.platform.ai.application.AiIntegrationException;
import com.aicareer.platform.ai.application.LlmResumeDraftClient;
import com.aicareer.platform.ai.application.LlmResumeDraftCompletion;
import com.aicareer.platform.ai.application.RetrievedContext;
import com.aicareer.platform.job.domain.JobPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a configured primary resume provider and uses a second provider only when the primary
 * cannot complete or validate a draft. Provider metadata is selected per request.
 */
public class FallbackLlmResumeDraftClient implements LlmResumeDraftClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmResumeDraftClient.class);

    private final LlmResumeDraftClient primary;
    private final LlmResumeDraftClient fallback;
    private final boolean fallbackEnabled;
    private final ThreadLocal<LlmResumeDraftClient> selected = new ThreadLocal<>();

    public FallbackLlmResumeDraftClient(
            LlmResumeDraftClient primary,
            LlmResumeDraftClient fallback,
            boolean fallbackEnabled
    ) {
        this.primary = primary;
        this.fallback = fallback;
        this.fallbackEnabled = fallbackEnabled;
    }

    @Override
    public LlmResumeDraftCompletion generateResumeDraft(JobPosting jobPosting, List<RetrievedContext> contexts) {
        selected.set(primary);
        try {
            return primary.generateResumeDraft(jobPosting, contexts);
        } catch (AiIntegrationException exception) {
            if (!fallbackEnabled || !shouldFallback(exception)) {
                throw exception;
            }
            log.warn("Primary resume draft provider {} failed with {} ({}); using fallback {}",
                    primary.provider(),
                    exception.kind(),
                    failureReason(exception),
                    fallback.provider());
            selected.set(fallback);
            try {
                return fallback.generateResumeDraft(jobPosting, contexts);
            } catch (AiIntegrationException fallbackException) {
                fallbackException.addSuppressed(exception);
                log.warn("Fallback resume draft provider {} failed with {} ({})",
                        fallback.provider(),
                        fallbackException.kind(),
                        failureReason(fallbackException));
                throw fallbackException;
            }
        }
    }

    @Override
    public String provider() {
        return active().provider();
    }

    @Override
    public String model() {
        return active().model();
    }

    @Override
    public String resumeDraftPromptVersion() {
        return active().resumeDraftPromptVersion();
    }

    private LlmResumeDraftClient active() {
        LlmResumeDraftClient client = selected.get();
        return client == null ? primary : client;
    }

    private boolean shouldFallback(AiIntegrationException exception) {
        return exception.kind() == AiIntegrationException.Kind.UNAVAILABLE
                || exception.kind() == AiIntegrationException.Kind.INCOMPLETE_RESPONSE
                || exception.kind() == AiIntegrationException.Kind.INVALID_RESPONSE;
    }

    private String failureReason(AiIntegrationException exception) {
        Throwable cause = exception.getCause();
        if (cause == null) {
            return exception.getMessage();
        }
        return exception.getMessage() + "; cause=" + cause.getClass().getSimpleName();
    }
}
