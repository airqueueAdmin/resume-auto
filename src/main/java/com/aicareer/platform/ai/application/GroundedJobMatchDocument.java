package com.aicareer.platform.ai.application;

import java.util.List;

public record GroundedJobMatchDocument(
        String summary,
        List<GroundedMatchFinding> strengths,
        List<GroundedMatchFinding> gaps,
        List<GroundedMatchFinding> recommendations
) {
    public GroundedJobMatchDocument {
        strengths = List.copyOf(strengths == null ? List.of() : strengths);
        gaps = List.copyOf(gaps == null ? List.of() : gaps);
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }
}
