package com.aicareer.platform.career.resume.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.api.CreateResumeRequest;
import com.aicareer.platform.career.resume.api.RenameResumeRequest;
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
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ResumeService(
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
    public Resume create(UUID userId, CreateResumeRequest request) {
        User user = getActiveUser(userId);
        Instant now = clock.instant();
        Resume resume = Resume.create(user, normalize(request.name()), now);
        int versionNumber = resume.nextVersionNumber();
        resume.markVersionCreated(versionNumber, now);
        resumeRepository.save(resume);
        resumeVersionRepository.save(ResumeVersion.create(
                resume,
                versionNumber,
                normalizeContent(request.content()),
                now
        ));
        return resume;
    }

    @Transactional(readOnly = true)
    public Page<Resume> list(UUID userId, Pageable pageable) {
        requireActiveUser(userId);
        return resumeRepository.findAllByUser_IdAndDeletedAtIsNull(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Resume get(UUID userId, UUID resumeId) {
        requireActiveUser(userId);
        return resumeRepository.findByIdAndUser_IdAndDeletedAtIsNull(resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    @Transactional
    public Resume rename(UUID userId, UUID resumeId, RenameResumeRequest request) {
        Resume resume = get(userId, resumeId);
        resume.rename(normalize(request.name()), clock.instant());
        return resume;
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId) {
        Resume resume = get(userId, resumeId);
        resume.softDelete(clock.instant());
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

    private String normalizeContent(String value) {
        return value.strip();
    }
}
