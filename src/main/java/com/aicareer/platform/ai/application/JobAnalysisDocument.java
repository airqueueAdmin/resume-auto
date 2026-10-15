package com.aicareer.platform.ai.application;

import java.util.List;

/** Structured output contract kept small and explicit for the first LLM use case. */
public record JobAnalysisDocument(
        String summary,
        String roleFamily,
        String seniority,
        List<String> responsibilities,
        List<String> requiredSkills,
        List<String> preferredSkills,
        List<String> keywords
) {
    public JobAnalysisDocument {
        responsibilities = List.copyOf(responsibilities == null ? List.of() : responsibilities);
        requiredSkills = List.copyOf(requiredSkills == null ? List.of() : requiredSkills);
        preferredSkills = List.copyOf(preferredSkills == null ? List.of() : preferredSkills);
        keywords = List.copyOf(keywords == null ? List.of() : keywords);
    }
}
