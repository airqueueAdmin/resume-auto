package com.aicareer.platform.ai.infrastructure.persistence;

import java.util.UUID;

import com.aicareer.platform.ai.domain.JobAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAnalysisRepository extends JpaRepository<JobAnalysis, UUID> {
}
