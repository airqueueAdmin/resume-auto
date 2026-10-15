package com.aicareer.platform.retrieval.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.aicareer.platform.career.experience.domain.Experience;
import com.aicareer.platform.career.experience.infrastructure.persistence.ExperienceRepository;
import com.aicareer.platform.career.project.domain.Project;
import com.aicareer.platform.career.project.infrastructure.persistence.ProjectRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.retrieval.domain.SourceType;
import com.aicareer.platform.retrieval.infrastructure.persistence.EmbeddingChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbeddingIndexService {

    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final Clock clock;

    public EmbeddingIndexService(
            ExperienceRepository experienceRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            EmbeddingClient embeddingClient,
            EmbeddingChunkRepository embeddingChunkRepository,
            Clock clock
    ) {
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.embeddingClient = embeddingClient;
        this.embeddingChunkRepository = embeddingChunkRepository;
        this.clock = clock;
    }

    @Transactional
    public EmbeddingIndexOutcome index(UUID userId) {
        requireActiveUser(userId);

        List<SourceDocument> documents = new ArrayList<>();
        experienceRepository.findAllByUser_IdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::experienceDocument)
                .forEach(documents::add);
        projectRepository.findAllByUser_IdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::projectDocument)
                .forEach(documents::add);

        List<double[]> embeddings = embeddingClient.embed(
                documents.stream()
                        .map(document -> "search_document: " + document.content())
                        .toList()
        );
        if (embeddings.size() != documents.size()) {
            throw EmbeddingIntegrationException.invalidResponse(
                    "Embedding count does not match source document count",
                    null
            );
        }

        embeddingChunkRepository.deleteAllForUser(userId);
        Timestamp now = Timestamp.from(clock.instant());
        for (int index = 0; index < documents.size(); index++) {
            SourceDocument document = documents.get(index);
            embeddingChunkRepository.insert(
                    UUID.randomUUID(),
                    userId,
                    document.sourceType() == SourceType.EXPERIENCE ? document.sourceId() : null,
                    document.sourceType() == SourceType.PROJECT ? document.sourceId() : null,
                    0,
                    document.content(),
                    sha256(document.content()),
                    embeddings.get(index),
                    embeddingClient.model(),
                    embeddingClient.dimensions(),
                    now,
                    now
            );
        }

        return new EmbeddingIndexOutcome(
                (int) documents.stream().filter(document -> document.sourceType() == SourceType.EXPERIENCE).count(),
                (int) documents.stream().filter(document -> document.sourceType() == SourceType.PROJECT).count(),
                embeddingClient.model(),
                embeddingClient.dimensions(),
                clock.instant()
        );
    }

    private SourceDocument experienceDocument(Experience experience) {
        String content = "Experience\n"
                + "Company: " + experience.getCompanyName() + "\n"
                + "Role: " + experience.getRole() + "\n"
                + "Description: " + experience.getDescription();
        return new SourceDocument(SourceType.EXPERIENCE, experience.getId(), content);
    }

    private SourceDocument projectDocument(Project project) {
        String period = project.getEndDate() == null
                ? project.getStartDate() + " - present"
                : project.getStartDate() + " - " + project.getEndDate();
        String repository = project.getRepositoryUrl() == null
                ? ""
                : "\nRepository: " + project.getRepositoryUrl();
        String customerOrDomain = project.getCustomerOrDomain() == null
                ? ""
                : "\nCustomer/Domain: " + project.getCustomerOrDomain();
        String content = "Project\n"
                + "Name: " + project.getName() + "\n"
                + "Role: " + project.getRole() + "\n"
                + "Period: " + period + "\n"
                + "Tech stack: " + project.getTechStack() + customerOrDomain + repository + "\n"
                + "Description: " + project.getDescription();
        return new SourceDocument(SourceType.PROJECT, project.getId(), content);
    }

    private User requireActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record SourceDocument(SourceType sourceType, UUID sourceId, String content) {
    }
}
