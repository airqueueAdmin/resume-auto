package com.aicareer.platform.retrieval.api;

import java.util.UUID;

import com.aicareer.platform.retrieval.application.EmbeddingIndexService;
import com.aicareer.platform.retrieval.application.VectorSearchService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retrieval")
@Tag(name = "Retrieval")
@SecurityRequirement(name = "bearerAuth")
public class RetrievalController {

    private final EmbeddingIndexService embeddingIndexService;
    private final VectorSearchService vectorSearchService;

    public RetrievalController(
            EmbeddingIndexService embeddingIndexService,
            VectorSearchService vectorSearchService
    ) {
        this.embeddingIndexService = embeddingIndexService;
        this.vectorSearchService = vectorSearchService;
    }

    @PostMapping("/index")
    public EmbeddingIndexResponse index(@AuthenticationPrincipal Jwt jwt) {
        return EmbeddingIndexResponse.from(
                embeddingIndexService.index(userId(jwt))
        );
    }

    @PostMapping("/search")
    public VectorSearchResponse search(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VectorSearchRequest request
    ) {
        return VectorSearchResponse.from(
                vectorSearchService.search(userId(jwt), request)
        );
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
