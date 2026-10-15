package com.aicareer.platform.ai.application;

import java.util.List;

/**
 * A self-introduction topic after the provider citation IDs have been resolved
 * to immutable source snapshots.
 */
public record GroundedResumeDraftSection(
        String title,
        String content,
        List<SourceCitation> citations
) {
    public GroundedResumeDraftSection {
        citations = List.copyOf(citations == null ? List.of() : citations);
    }
}
