package com.aicareer.platform.identity.infrastructure.persistence;

import java.util.UUID;

import com.aicareer.platform.identity.domain.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
}
