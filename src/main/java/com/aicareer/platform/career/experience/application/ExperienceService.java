package com.aicareer.platform.career.experience.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.experience.api.ExperienceRequest;
import com.aicareer.platform.career.experience.domain.Experience;
import com.aicareer.platform.career.experience.infrastructure.persistence.ExperienceRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ExperienceService(
            ExperienceRepository experienceRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.experienceRepository = experienceRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public Experience create(UUID userId, ExperienceRequest request) {
        User user = getActiveUser(userId);
        Instant now = clock.instant();
        return experienceRepository.save(Experience.create(
                user,
                normalize(request.companyName()),
                normalize(request.role()),
                normalizeOptional(request.department()),
                normalizeOptional(request.title()),
                request.startDate(),
                request.endDate(),
                Boolean.TRUE.equals(request.currentlyEmployed()),
                normalize(request.description()),
                normalizeOptional(request.techStack()),
                now
        ));
    }

    @Transactional(readOnly = true)
    public Page<Experience> list(UUID userId, Pageable pageable) {
        requireActiveUser(userId);
        return experienceRepository.findAllByUser_IdAndDeletedAtIsNull(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Experience get(UUID userId, UUID experienceId) {
        requireActiveUser(userId);
        return experienceRepository.findByIdAndUser_IdAndDeletedAtIsNull(experienceId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    @Transactional
    public Experience update(UUID userId, UUID experienceId, ExperienceRequest request) {
        Experience experience = get(userId, experienceId);
        experience.update(
                normalize(request.companyName()),
                normalize(request.role()),
                normalizeOptional(request.department()),
                normalizeOptional(request.title()),
                request.startDate(),
                request.endDate(),
                Boolean.TRUE.equals(request.currentlyEmployed()),
                normalize(request.description()),
                normalizeOptional(request.techStack()),
                clock.instant()
        );
        return experience;
    }

    @Transactional
    public void delete(UUID userId, UUID experienceId) {
        Experience experience = get(userId, experienceId);
        experience.softDelete(clock.instant());
    }

    private User getActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }

    private void requireActiveUser(UUID userId) {
        getActiveUser(userId);
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
