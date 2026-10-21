package com.aicareer.platform.ai.infrastructure.llm;

import java.util.List;

import com.aicareer.platform.ai.application.AiIntegrationException;
import com.aicareer.platform.ai.application.GroundingValidationException;
import com.aicareer.platform.ai.application.LlmSelfIntroductionAnswerClient;
import com.aicareer.platform.ai.application.LlmSelfIntroductionAnswerCompletion;
import com.aicareer.platform.ai.application.RetrievedContext;
import com.aicareer.platform.ai.application.SelfIntroductionJobContext;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retries unusable Gemini self-introduction output with the configured local Ollama model. */
public class FallbackLlmSelfIntroductionAnswerClient implements LlmSelfIntroductionAnswerClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmSelfIntroductionAnswerClient.class);

    private final LlmSelfIntroductionAnswerClient primary;
    private final LlmSelfIntroductionAnswerClient fallback;
    private final boolean fallbackEnabled;
    private final ThreadLocal<LlmSelfIntroductionAnswerClient> selected = new ThreadLocal<>();

    public FallbackLlmSelfIntroductionAnswerClient(
            LlmSelfIntroductionAnswerClient primary,
            LlmSelfIntroductionAnswerClient fallback,
            boolean fallbackEnabled
    ) {
        this.primary = primary;
        this.fallback = fallback;
        this.fallbackEnabled = fallbackEnabled;
    }

    @Override
    public LlmSelfIntroductionAnswerCompletion generateAnswer(
            SelfIntroductionJobContext jobContext,
            String question,
            List<RetrievedContext> contexts
    ) {
        selected.set(primary);
        try {
            return generateAndValidate(primary, jobContext, question, contexts);
        } catch (AiIntegrationException | GroundingValidationException exception) {
            if (!fallbackEnabled || !shouldFallback(exception)) {
                throw exception;
            }
            log.warn("Primary self-introduction provider {} failed kind={}; retrying with fallback provider {}",
                    primary.provider(), failureKind(exception), fallback.provider());
            selected.set(fallback);
            try {
                return generateAndValidate(fallback, jobContext, question, contexts);
            } catch (AiIntegrationException | GroundingValidationException fallbackException) {
                fallbackException.addSuppressed(exception);
                log.warn("Fallback self-introduction provider {} failed kind={}",
                        fallback.provider(), failureKind(fallbackException));
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
    public String selfIntroductionPromptVersion() {
        return active().selfIntroductionPromptVersion();
    }

    private LlmSelfIntroductionAnswerClient active() {
        LlmSelfIntroductionAnswerClient client = selected.get();
        return client == null ? primary : client;
    }

    private LlmSelfIntroductionAnswerCompletion generateAndValidate(
            LlmSelfIntroductionAnswerClient client, SelfIntroductionJobContext jobContext,
            String question, List<RetrievedContext> contexts
    ) {
        LlmSelfIntroductionAnswerCompletion completion = client.generateAnswer(jobContext, question, contexts);
        SelfIntroductionAnswerValidator.validate(completion, contexts);
        return completion;
    }

    private boolean shouldFallback(RuntimeException exception) {
        if (exception instanceof GroundingValidationException grounding) {
            return grounding.kind() == GroundingValidationException.Kind.UNKNOWN_CITATION
                    || grounding.kind() == GroundingValidationException.Kind.INVALID_FINDING
                    || grounding.kind() == GroundingValidationException.Kind.UNCITED_FINDING;
        }
        AiIntegrationException integration = (AiIntegrationException) exception;
        return integration.kind() == AiIntegrationException.Kind.INCOMPLETE_RESPONSE
                || integration.kind() == AiIntegrationException.Kind.INVALID_RESPONSE
                || integration.kind() == AiIntegrationException.Kind.UNAVAILABLE;
    }

    private Object failureKind(RuntimeException exception) {
        return exception instanceof GroundingValidationException grounding
                ? grounding.kind() : ((AiIntegrationException) exception).kind();
    }
}
