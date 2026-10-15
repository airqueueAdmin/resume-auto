package com.aicareer.platform.career.project.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.project.api.ProjectRequest;
import com.aicareer.platform.career.project.domain.Project;
import com.aicareer.platform.career.project.infrastructure.persistence.ProjectRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public Project create(UUID userId, ProjectRequest request) {
        User user = getActiveUser(userId);
        Instant now = clock.instant();
        return projectRepository.save(Project.create(
                user,
                normalize(request.name()),
                normalize(request.role()),
                request.startDate(),
                request.endDate(),
                normalize(request.description()),
                normalize(request.techStack()),
                normalizeOptional(request.repositoryUrl()),
                now
        ));
    }

    @Transactional(readOnly = true)
    public Page<Project> list(UUID userId, Pageable pageable) {
        requireActiveUser(userId);
        return projectRepository.findAllByUser_IdAndDeletedAtIsNull(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Project get(UUID userId, UUID projectId) {
        requireActiveUser(userId);
        return projectRepository.findByIdAndUser_IdAndDeletedAtIsNull(projectId, userId)
                .orElseThrow(CareerResourceNotFoundException::new);
    }

    @Transactional
    public Project update(UUID userId, UUID projectId, ProjectRequest request) {
        Project project = get(userId, projectId);
        project.update(
                normalize(request.name()),
                normalize(request.role()),
                request.startDate(),
                request.endDate(),
                normalize(request.description()),
                normalize(request.techStack()),
                normalizeOptional(request.repositoryUrl()),
                clock.instant()
        );
        return project;
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        Project project = get(userId, projectId);
        project.softDelete(clock.instant());
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
