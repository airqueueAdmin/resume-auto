package com.aicareer.platform.career.resume.question.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.career.resume.question.domain.ResumeApplicationQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeApplicationQuestionRepository extends JpaRepository<ResumeApplicationQuestion, UUID> {

    Page<ResumeApplicationQuestion> findAllByResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(
            UUID resumeId,
            UUID userId,
            Pageable pageable
    );

    Optional<ResumeApplicationQuestion> findByIdAndResume_IdAndResume_User_IdAndResume_DeletedAtIsNull(
            UUID id,
            UUID resumeId,
            UUID userId
    );
}
