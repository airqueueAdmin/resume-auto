package com.aicareer.platform.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.email = :email and user.deletedAt is null")
    Optional<User> findByEmailAndDeletedAtIsNullForUpdate(@Param("email") String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
