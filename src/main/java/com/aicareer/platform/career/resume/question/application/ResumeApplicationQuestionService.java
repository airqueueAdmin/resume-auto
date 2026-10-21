package com.aicareer.platform.career.resume.question.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.domain.Resume;
import com.aicareer.platform.career.resume.infrastructure.persistence.ResumeRepository;
import com.aicareer.platform.career.resume.question.api.ResumeApplicationQuestionRequest;
import com.aicareer.platform.career.resume.question.domain.ResumeApplicationQuestion;
import com.aicareer.platform.career.resume.question.infrastructure.persistence.ResumeApplicationQuestionRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.job.domain.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeApplicationQuestionService {

    private final ResumeRepository resumeRepository;
    private final ResumeApplicationQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final JobPostingService jobPostingService;
    private final Clock clock;

    public ResumeApplicationQuestionService(
            ResumeRepository resumeRepository,
            ResumeApplicationQuestionRepository questionRepository,
            UserRepository userRepository,
            JobPostingService jobPostingService,
            Clock clock
    ) {
        this.resumeRepository = resumeRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.jobPostingService = jobPostingService;
        this.clock = clock;
    }

    @Transactional
    public ResumeApplicationQuestion create(
            UUID userId,
            UUID resumeId,
            ResumeApplicationQuestionRequest request
    ) {
        Resume resume = requireResume(userId, resumeId);
        JobPosting jobPosting = jobPostingService.get(userId, request.jobPostingId());
        Instant now = clock.instant();
        ResumeApplicationQuestion question = questionRepository.save(ResumeApplicationQuestion.create(
                resume,
                jobPosting,
                normalizeQuestion(request.question()),
                normalizeAnswer(request.answer()),
                now
        ));
        resume.touch(now);
        return question;
    }

    @Transactional(readOnly = true)
    public Page<ResumeApplicationQuestion> list(UUID userId, UUID resumeId, Pageable pageable) {
        requireResume(userId, resumeId);
        return questionRepository.findAllByResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(
                resumeId,
                userId,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public ResumeApplicationQuestion get(UUID userId, UUID resumeId, UUID questionId) {
        requireResume(userId, resumeId);
        return questionRepository
                .findByIdAndResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(questionId, resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    @Transactional
    public ResumeApplicationQuestion update(
            UUID userId,
            UUID resumeId,
            UUID questionId,
            ResumeApplicationQuestionRequest request
    ) {
        requireActiveUser(userId);
        ResumeApplicationQuestion question = questionRepository
                .findByIdAndResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(questionId, resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
        JobPosting jobPosting = jobPostingService.get(userId, request.jobPostingId());
        Instant now = clock.instant();
        question.update(
                jobPosting,
                normalizeQuestion(request.question()),
                normalizeAnswer(request.answer()),
                now
        );
        requireResume(userId, resumeId).touch(now);
        return question;
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId, UUID questionId) {
        requireActiveUser(userId);
        ResumeApplicationQuestion question = questionRepository
                .findByIdAndResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(questionId, resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
        questionRepository.delete(question);
        requireResume(userId, resumeId).touch(clock.instant());
    }

    private Resume requireResume(UUID userId, UUID resumeId) {
        requireActiveUser(userId);
        return resumeRepository.findByIdAndUser_IdAndDeletedAtIsNull(resumeId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    private User requireActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }

    private String normalizeQuestion(String value) {
        return value.trim();
    }

    private String normalizeAnswer(String value) {
        return value == null ? "" : value.strip();
    }
}
