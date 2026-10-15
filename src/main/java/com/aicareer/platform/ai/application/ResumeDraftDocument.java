package com.aicareer.platform.ai.application;

import java.util.List;

public record ResumeDraftDocument(
        ResumeDraftLine headline,
        ResumeDraftLine summary,
        List<ResumeDraftSection> selfIntroductionSections,
        List<ResumeDraftLine> experienceBullets,
        List<ResumeDraftLine> projectBullets,
        List<ResumeDraftLine> skills
) {
    public ResumeDraftDocument {
        selfIntroductionSections = List.copyOf(
                selfIntroductionSections == null ? List.of() : selfIntroductionSections
        );
        experienceBullets = List.copyOf(experienceBullets == null ? List.of() : experienceBullets);
        projectBullets = List.copyOf(projectBullets == null ? List.of() : projectBullets);
        skills = List.copyOf(skills == null ? List.of() : skills);
    }

    /**
     * Keeps older provider fixtures and stored examples source-compatible. New provider responses
     * must use the structured self-introduction sections validated by ResumeDraftService.
     */
    public ResumeDraftDocument(
            ResumeDraftLine headline,
            ResumeDraftLine summary,
            List<ResumeDraftLine> experienceBullets,
            List<ResumeDraftLine> projectBullets,
            List<ResumeDraftLine> skills
    ) {
        this(headline, summary, List.of(), experienceBullets, projectBullets, skills);
    }
}
