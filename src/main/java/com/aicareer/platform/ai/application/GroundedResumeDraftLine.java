package com.aicareer.platform.ai.application;

import java.util.List;

public record GroundedResumeDraftLine(
        String text,
        List<SourceCitation> citations
) {
    public GroundedResumeDraftLine {
        citations = List.copyOf(citations == null ? List.of() : citations);
    }
}
