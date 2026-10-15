package com.aicareer.platform.identity.application;

import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }
}
