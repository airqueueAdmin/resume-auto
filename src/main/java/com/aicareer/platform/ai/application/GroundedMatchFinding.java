package com.aicareer.platform.ai.application;

import java.util.List;

public record GroundedMatchFinding(
        String statement,
        List<SourceCitation> citations
) {
    public GroundedMatchFinding {
        citations = List.copyOf(citations == null ? List.of() : citations);
    }
}
