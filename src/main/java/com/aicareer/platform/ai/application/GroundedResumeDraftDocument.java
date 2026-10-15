package com.aicareer.platform.ai.application;

import java.util.List;

public record GroundedResumeDraftDocument(
        GroundedResumeDraftLine headline,
        GroundedResumeDraftLine summary,
        List<GroundedResumeDraftSection> selfIntroductionSections,
        List<GroundedResumeDraftLine> experienceBullets,
        List<GroundedResumeDraftLine> projectBullets,
        List<GroundedResumeDraftLine> skills
) {
    public GroundedResumeDraftDocument {
        selfIntroductionSections = List.copyOf(
                selfIntroductionSections == null ? List.of() : selfIntroductionSections
        );
        experienceBullets = List.copyOf(experienceBullets == null ? List.of() : experienceBullets);
        projectBullets = List.copyOf(projectBullets == null ? List.of() : projectBullets);
        skills = List.copyOf(skills == null ? List.of() : skills);
    }

    public GroundedResumeDraftDocument(
            GroundedResumeDraftLine headline,
            GroundedResumeDraftLine summary,
            List<GroundedResumeDraftLine> experienceBullets,
            List<GroundedResumeDraftLine> projectBullets,
            List<GroundedResumeDraftLine> skills
    ) {
        this(headline, summary, List.of(), experienceBullets, projectBullets, skills);
    }
}
