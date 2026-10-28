package com.aicareer.platform.career.resume.question.domain;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.resume.domain.Resume;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.shared.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resume_application_questions")
public class ResumeApplicationQuestion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ResumeApplicationQuestion() {
    }

    private ResumeApplicationQuestion(
            Resume resume,
            JobPosting jobPosting,
            String question,
            String answer,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.resume = resume;
        this.jobPosting = jobPosting;
        this.question = question;
        this.answer = answer;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ResumeApplicationQuestion create(
            Resume resume,
            JobPosting jobPosting,
            String question,
            String answer,
            Instant now
    ) {
        return new ResumeApplicationQuestion(resume, jobPosting, question, answer, now);
    }

    public void update(JobPosting jobPosting, String question, String answer, Instant now) {
        this.jobPosting = jobPosting;
        this.question = question;
        this.answer = answer;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getResumeId() {
        return resume.getId();
    }

    public UUID getJobPostingId() {
        return jobPosting == null ? null : jobPosting.getId();
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
