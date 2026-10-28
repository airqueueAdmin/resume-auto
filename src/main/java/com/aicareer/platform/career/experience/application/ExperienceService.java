package com.aicareer.platform.career.experience.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.experience.api.ExperienceRequest;
import com.aicareer.platform.career.experience.domain.Experience;
import com.aicareer.platform.career.experience.infrastructure.persistence.ExperienceRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
        // startDate/endDate are encrypted at rest, so PostgreSQL cannot safely sort them.
        // Decrypting only this user's rows and applying the existing business order here
        // preserves pagination without exposing a plaintext sort column in the database.
        List<Experience> all = experienceRepository.findAllByUser_IdAndDeletedAtIsNull(userId).stream()
                .sorted(Comparator
                        .comparing(Experience::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Experience::isCurrentlyEmployed, Comparator.reverseOrder())
                        .thenComparing(Experience::getEndDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Experience::getId, Comparator.reverseOrder()))
                .toList();

        long offset = pageable.getOffset();
        int from = offset >= all.size() ? all.size() : (int) offset;
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
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
