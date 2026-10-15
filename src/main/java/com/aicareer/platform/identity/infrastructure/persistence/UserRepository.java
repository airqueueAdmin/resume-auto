package com.aicareer.platform.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
