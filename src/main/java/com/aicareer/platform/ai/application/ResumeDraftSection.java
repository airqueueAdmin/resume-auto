package com.aicareer.platform.ai.application;

import java.util.List;

/**
 * A major self-introduction topic returned by the resume-draft provider.
 */
public record ResumeDraftSection(
        String title,
        String content,
        List<String> citationIds
) {
    public ResumeDraftSection {
        citationIds = List.copyOf(citationIds == null ? List.of() : citationIds);
    }
}
