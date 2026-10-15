package com.aicareer.platform.ai.application;

import java.util.List;

public record JobMatchDocument(
        String summary,
        List<MatchFinding> strengths,
        List<MatchFinding> gaps,
        List<MatchFinding> recommendations
) {
    public JobMatchDocument {
        strengths = List.copyOf(strengths == null ? List.of() : strengths);
        gaps = List.copyOf(gaps == null ? List.of() : gaps);
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }
}
