package com.aicareer.platform.ai.application;

import java.util.List;

public record ResumeDraftLine(
        String text,
        List<String> citationIds
) {
    public ResumeDraftLine {
        citationIds = List.copyOf(citationIds == null ? List.of() : citationIds);
    }
}
