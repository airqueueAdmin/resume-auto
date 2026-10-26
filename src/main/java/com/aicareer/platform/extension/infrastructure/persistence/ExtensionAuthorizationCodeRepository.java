package com.aicareer.platform.extension.infrastructure.persistence;

import java.util.Optional;

import com.aicareer.platform.extension.domain.ExtensionAuthorizationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ExtensionAuthorizationCodeRepository
        extends JpaRepository<ExtensionAuthorizationCode, java.util.UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExtensionAuthorizationCode> findByCodeHash(String codeHash);
}
