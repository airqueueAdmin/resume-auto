package com.aicareer.platform.ai.application;

import java.util.List;

public interface LlmSelfIntroductionAnswerClient {

    LlmSelfIntroductionAnswerCompletion generateAnswer(
            SelfIntroductionJobContext jobContext,
            String question,
            List<RetrievedContext> contexts
    );

    String provider();

    String model();

    String selfIntroductionPromptVersion();
}
