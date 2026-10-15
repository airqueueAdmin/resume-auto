package com.aicareer.platform.job.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.job.api.JobPostingRequest;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.job.infrastructure.persistence.JobPostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public JobPostingService(
            JobPostingRepository jobPostingRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public JobPosting create(UUID userId, JobPostingRequest request) {
        User user = getActiveUser(userId);
        Instant now = clock.instant();
        return jobPostingRepository.save(JobPosting.create(
                user,
                normalize(request.title()),
                normalize(request.companyName()),
                normalize(request.description()),
                normalizeOptional(request.sourceUrl()),
                now
        ));
    }

    @Transactional(readOnly = true)
    public Page<JobPosting> list(UUID userId, Pageable pageable) {
        requireActiveUser(userId);
        return jobPostingRepository.findAllByUser_IdAndDeletedAtIsNull(userId, pageable);
    }

    @Transactional(readOnly = true)
    public JobPosting get(UUID userId, UUID jobPostingId) {
        requireActiveUser(userId);
        return jobPostingRepository.findByIdAndUser_IdAndDeletedAtIsNull(jobPostingId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    @Transactional
    public JobPosting update(UUID userId, UUID jobPostingId, JobPostingRequest request) {
        JobPosting jobPosting = get(userId, jobPostingId);
        jobPosting.update(
                normalize(request.title()),
                normalize(request.companyName()),
                normalize(request.description()),
                normalizeOptional(request.sourceUrl()),
                clock.instant()
        );
        return jobPosting;
    }

    @Transactional
    public void delete(UUID userId, UUID jobPostingId) {
        JobPosting jobPosting = get(userId, jobPostingId);
        jobPosting.softDelete(clock.instant());
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
        return value == null || value.isBlank() ? null : value.trim();
    }
}
