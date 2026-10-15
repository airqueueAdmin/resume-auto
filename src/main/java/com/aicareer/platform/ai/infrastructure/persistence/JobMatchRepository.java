package com.aicareer.platform.ai.infrastructure.persistence;

import java.util.UUID;

import com.aicareer.platform.ai.domain.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
}
