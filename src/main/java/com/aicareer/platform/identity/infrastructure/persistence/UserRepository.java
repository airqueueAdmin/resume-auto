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

    boolean existsByEmailHash(String emailHash);

    Optional<User> findByEmailHashAndDeletedAtIsNull(String emailHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.emailHash = :emailHash and user.deletedAt is null")
    Optional<User> findByEmailHashAndDeletedAtIsNullForUpdate(@Param("emailHash") String emailHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id and user.deletedAt is null")
    Optional<User> findByIdAndDeletedAtIsNullForUpdate(@Param("id") UUID id);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
