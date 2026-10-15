package com.aicareer.platform.retrieval.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.retrieval.api.VectorSearchRequest;
import com.aicareer.platform.retrieval.domain.SourceType;
import com.aicareer.platform.retrieval.infrastructure.persistence.EmbeddingChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSearchService {

    private final UserRepository userRepository;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingChunkRepository embeddingChunkRepository;

    public VectorSearchService(
            UserRepository userRepository,
            EmbeddingClient embeddingClient,
            EmbeddingChunkRepository embeddingChunkRepository
    ) {
        this.userRepository = userRepository;
        this.embeddingClient = embeddingClient;
        this.embeddingChunkRepository = embeddingChunkRepository;
    }

    @Transactional(readOnly = true)
    public VectorSearchOutcome search(UUID userId, VectorSearchRequest request) {
        return search(
                userId,
                request.query(),
                request.effectiveTopK(),
                request.effectiveSourceTypes()
        );
    }

    @Transactional(readOnly = true)
    public VectorSearchOutcome search(UUID userId, String query, int topK) {
        return search(userId, query, topK, Set.of());
    }

    @Transactional(readOnly = true)
    public List<EmbeddingChunkSearchResult> searchAll(UUID userId) {
        requireActiveUser(userId);
        return embeddingChunkRepository.findAllActiveForUser(userId);
    }

    private VectorSearchOutcome search(
            UUID userId,
            String query,
            int topK,
            Set<SourceType> sourceTypes
    ) {
        requireActiveUser(userId);
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
        if (!embeddingChunkRepository.existsActiveForUser(userId, sourceTypes)) {
            return new VectorSearchOutcome(
                    List.of(),
                    embeddingClient.model(),
                    embeddingClient.dimensions()
            );
        }
        List<double[]> embeddings = embeddingClient.embed(
                List.of("search_query: " + query.trim())
        );
        if (embeddings.size() != 1) {
            throw EmbeddingIntegrationException.invalidResponse(
                    "Query embedding count is invalid",
                    null
            );
        }

        return new VectorSearchOutcome(
                embeddingChunkRepository.search(
                        userId,
                        embeddings.get(0),
                        topK,
                        sourceTypes
                ),
                embeddingClient.model(),
                embeddingClient.dimensions()
        );
    }

    private User requireActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }
}
