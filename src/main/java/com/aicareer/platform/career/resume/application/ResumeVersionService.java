package com.aicareer.platform.career.resume.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.api.CreateResumeVersionRequest;
import com.aicareer.platform.career.resume.domain.Resume;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.career.resume.infrastructure.persistence.ResumeRepository;
import com.aicareer.platform.career.resume.infrastructure.persistence.ResumeVersionRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeVersionService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ResumeVersionService(
            ResumeRepository resumeRepository,
            ResumeVersionRepository resumeVersionRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public ResumeVersion create(UUID userId, UUID resumeId, CreateResumeVersionRequest request) {
        return createFromContent(userId, resumeId, request.content());
    }

    @Transactional
    public ResumeVersion createFromContent(UUID userId, UUID resumeId, String content) {
        requireActiveUser(userId);
        Resume resume = resumeRepository.findForUpdate(resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
        int versionNumber = resume.nextVersionNumber();
        Instant now = clock.instant();
        ResumeVersion version = resumeVersionRepository.save(ResumeVersion.create(
                resume,
                versionNumber,
                normalizeContent(content),
                now
        ));
        resume.markVersionCreated(versionNumber, now);
        return version;
    }

    @Transactional(readOnly = true)
    public Page<ResumeVersion> list(UUID userId, UUID resumeId, Pageable pageable) {
        requireActiveUser(userId);
        requireResume(userId, resumeId);
        return resumeVersionRepository
                .findAllByResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(resumeId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public ResumeVersion get(UUID userId, UUID resumeId, UUID versionId) {
        requireActiveUser(userId);
        return resumeVersionRepository
                .findByIdAndResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(versionId, resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    private Resume requireResume(UUID userId, UUID resumeId) {
        return resumeRepository.findByIdAndUser_IdAndDeletedAtIsNull(resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    private User getActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }

    private void requireActiveUser(UUID userId) {
        getActiveUser(userId);
    }

    private String normalizeContent(String value) {
        return value.strip();
    }
}
