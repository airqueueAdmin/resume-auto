package com.aicareer.platform.ai.application;

import java.util.List;

public record SelfIntroductionAnswerDocument(
        List<Sentence> sentences
) {
    public SelfIntroductionAnswerDocument {
        sentences = List.copyOf(sentences == null ? List.of() : sentences);
    }

    public record Sentence(
            String text,
            List<String> citationIds
    ) {
        public Sentence {
            citationIds = List.copyOf(citationIds == null ? List.of() : citationIds);
        }
    }
}
