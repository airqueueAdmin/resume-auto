package com.aicareer.platform.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.identity.domain.EmailVerificationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationCode> findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);
}
