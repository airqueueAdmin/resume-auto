package com.aicareer.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.aicareer.platform.ai.application.GroundedResumeDraftDocument;
import com.aicareer.platform.ai.application.GroundedResumeDraftLine;
import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.SourceCitation;
import com.aicareer.platform.ai.domain.ResumeDraft;
import com.aicareer.platform.ai.infrastructure.persistence.ResumeDraftRepository;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.job.infrastructure.persistence.JobPostingRepository;
import com.aicareer.platform.retrieval.application.EmbeddingChunkSearchResult;
import com.aicareer.platform.retrieval.domain.SourceType;
import com.aicareer.platform.retrieval.infrastructure.persistence.EmbeddingChunkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AiCareerPlatformApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("ai_career_test")
            .withUsername("ai_career_test")
            .withPassword("ai_career_test");

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private EmbeddingChunkRepository embeddingChunkRepository;

    @Autowired
    private ResumeDraftRepository resumeDraftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void contextLoads() {
        assertThat(port).isPositive();
    }

    @Test
    void actuatorHealthIsAvailable() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void actuatorLivenessAndReadinessProbesArePublic() throws Exception {
        HttpResponse<String> liveness = get("/actuator/health/liveness", null);
        HttpResponse<String> readiness = get("/actuator/health/readiness", null);

        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(liveness.body()).contains("\"status\":\"UP\"");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void openApiDocumentationIsPublicAndDescribesTheApi() throws Exception {
        HttpResponse<String> openApi = get("/v3/api-docs", null);
        HttpResponse<String> swaggerUi = get("/swagger-ui/index.html", null);

        assertThat(openApi.statusCode()).isEqualTo(200);
        assertThat(openApi.body())
                .contains("AI Career Platform API")
                .contains("/api/v1/auth/login")
                .contains("/api/v1/job-postings")
                .contains("/api/v1/resumes")
                .contains("/api/v1/ai/resume-drafts/{draftId}/approve")
                .contains("/api/v1/ai/resume-drafts/{draftId}/approve-new")
                .contains("/api/v1/jobkorea/export-audits")
                .contains("/api/v1/workflows/job-application-context")
                .contains("/api/v1/tools/calls")
                .contains("bearerAuth");
        assertThat(swaggerUi.statusCode()).isEqualTo(200);
        assertThat(swaggerUi.body()).contains("Swagger UI");
    }

    @Test
    void connectsToPostgresql() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select version()")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).startsWith("PostgreSQL 17");
        }
    }

    @Test
    void flywayAppliesIdentityCareerResumeJobPostingAiMatchingResumeDraftAndToolMigrations() throws Exception {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).hasSize(12);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                        and table_name in ('users', 'refresh_sessions', 'experiences', 'projects',
                                           'resumes', 'resume_versions', 'job_postings', 'ai_job_analyses',
                                           'embedding_chunks', 'ai_job_matches', 'ai_resume_drafts', 'tool_approvals',
                                           'jobkorea_export_audits')
                        """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(13);
        }
    }

    @Test
    void jobMatchEnforcesOwnershipAndRequiresRetrievedEvidence() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/job-postings", """
                {
                  "title": "Backend Developer",
                  "companyName": "Example Corp",
                  "description": "Build Java APIs and PostgreSQL services."
                }
                """, ownerToken);
        String jobPostingId = jsonValue(created.body(), "id");

        HttpResponse<String> crossUser = post("/api/v1/ai/match-job", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId), otherUserToken);
        HttpResponse<String> noEvidence = post("/api/v1/ai/match-job", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId), ownerToken);
        HttpResponse<String> unauthenticated = post("/api/v1/ai/match-job", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(crossUser.statusCode()).isEqualTo(404);
        assertThat(noEvidence.statusCode()).isEqualTo(422);
        assertThat(noEvidence.body()).contains("RAG_CONTEXT_EMPTY");
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
    }

    @Test
    void resumeDraftEnforcesOwnershipAndRequiresRetrievedEvidence() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/job-postings", """
                {
                  "title": "Backend Developer",
                  "companyName": "Example Corp",
                  "description": "Build Java APIs and PostgreSQL services."
                }
                """, ownerToken);
        String jobPostingId = jsonValue(created.body(), "id");

        HttpResponse<String> crossUser = post("/api/v1/ai/generate-resume-draft", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId), otherUserToken);
        HttpResponse<String> noEvidence = post("/api/v1/ai/generate-resume-draft", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId), ownerToken);
        HttpResponse<String> unauthenticated = post("/api/v1/ai/generate-resume-draft", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(crossUser.statusCode()).isEqualTo(404);
        assertThat(noEvidence.statusCode()).isEqualTo(422);
        assertThat(noEvidence.body()).contains("RAG_CONTEXT_EMPTY");
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
    }

    @Test
    void groundedResumeDraftApprovalCreatesNextImmutableVersionAndEnforcesOwnership() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        String ownerUserId = jsonValue(get("/api/v1/users/me", ownerToken).body(), "id");
        HttpResponse<String> job = post("/api/v1/job-postings", """
                {
                  "title": "Backend Developer",
                  "companyName": "Example Corp",
                  "description": "Build Java APIs and PostgreSQL services."
                }
                """, ownerToken);
        String jobId = jsonValue(job.body(), "id");
        HttpResponse<String> resume = post("/api/v1/resumes", """
                {
                  "name": "Approved Resume",
                  "content": "Initial resume content"
                }
                """, ownerToken);
        String resumeId = jsonValue(resume.body(), "id");

        User owner = userRepository.findById(UUID.fromString(ownerUserId)).orElseThrow();
        JobPosting jobPosting = jobPostingRepository.findById(UUID.fromString(jobId)).orElseThrow();
        SourceCitation citation = new SourceCitation(
                "C1",
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.randomUUID(),
                0,
                "Built Java APIs for production services.",
                0.94,
                "nomic-embed-text"
        );
        GroundedResumeDraftLine line = new GroundedResumeDraftLine(
                "Built Java APIs",
                List.of(citation)
        );
        GroundedResumeDraftDocument document = new GroundedResumeDraftDocument(
                line,
                line,
                List.of(line),
                List.of(),
                List.of(line)
        );
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ResumeDraft draft = resumeDraftRepository.saveAndFlush(ResumeDraft.create(
                owner,
                jobPosting,
                "ollama",
                "llama3.1:8b",
                "resume-draft-v1",
                "approval-test-response",
                mapper.valueToTree(document),
                mapper.valueToTree(List.of(citation)),
                1,
                new LlmUsage(10, 20, 30),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        HttpResponse<String> crossUser = post("/api/v1/ai/resume-drafts/%s/approve".formatted(draft.getId()), """
                {"resumeId":"%s"}
                """.formatted(resumeId), otherUserToken);
        HttpResponse<String> invalidApproval = post(
                "/api/v1/ai/resume-drafts/%s/approve".formatted(draft.getId()),
                "{}",
                ownerToken
        );
        HttpResponse<String> approved = post("/api/v1/ai/resume-drafts/%s/approve".formatted(draft.getId()), """
                {"resumeId":"%s"}
                """.formatted(resumeId), ownerToken);
        HttpResponse<String> autoCreatedResume = post(
                "/api/v1/ai/resume-drafts/%s/approve-new".formatted(draft.getId()),
                "{\"resumeName\":\"Example Corp - Backend 이력서\"}",
                ownerToken
        );
        HttpResponse<String> autoCreatedCrossUser = post(
                "/api/v1/ai/resume-drafts/%s/approve-new".formatted(draft.getId()),
                "{\"resumeName\":\"Should Not Exist\"}",
                otherUserToken
        );
        String autoCreatedResumeId = jsonValue(autoCreatedResume.body(), "id");
        HttpResponse<String> autoCreatedVersions = get(
                "/api/v1/resumes/%s/versions?page=0&size=20".formatted(autoCreatedResumeId),
                ownerToken
        );
        String approvedVersionId = jsonValue(approved.body(), "id");
        HttpResponse<String> versions = get("/api/v1/resumes/%s/versions?page=0&size=20".formatted(resumeId), ownerToken);
        HttpResponse<String> attemptedUpdate = put(
                "/api/v1/resumes/%s/versions/%s".formatted(resumeId, approvedVersionId),
                "{\"content\":\"Must not mutate approved version\"}",
                ownerToken
        );

        assertThat(job.statusCode()).isEqualTo(201);
        assertThat(resume.statusCode()).isEqualTo(201);
        assertThat(crossUser.statusCode()).isEqualTo(404);
        assertThat(invalidApproval.statusCode()).isEqualTo(400);
        assertThat(invalidApproval.body()).contains("VALIDATION_ERROR", "resumeId");
        assertThat(approved.statusCode()).isEqualTo(201);
        assertThat(approved.body())
                .contains("Built Java APIs", "[C1]", "\"versionNumber\":2");
        assertThat(autoCreatedResume.statusCode()).isEqualTo(201);
        assertThat(autoCreatedResume.body())
                .contains("Example Corp - Backend 이력서", "\"latestVersionNumber\":1");
        assertThat(autoCreatedCrossUser.statusCode()).isEqualTo(404);
        assertThat(autoCreatedVersions.statusCode()).isEqualTo(200);
        assertThat(autoCreatedVersions.body())
                .contains("\"totalElements\":1", "Built Java APIs", "[C1]");
        assertThat(versions.statusCode()).isEqualTo(200);
        assertThat(versions.body()).contains("\"totalElements\":2", "Initial resume content");
        assertThat(attemptedUpdate.statusCode()).isEqualTo(405);
    }

    @Test
    void toolsEnforceAllowlistOwnershipArgumentValidationAndOneTimeApproval() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        String experienceId = jsonValue(post("/api/v1/experiences", """
                {
                  "companyName": "Owner Corp",
                  "role": "Backend Developer",
                  "startDate": "2022-01-01",
                  "description": "Private owner experience."
                }
                """, ownerToken).body(), "id");

        HttpResponse<String> unauthenticatedCatalog = get("/api/v1/tools", null);
        HttpResponse<String> catalog = get("/api/v1/tools", ownerToken);
        HttpResponse<String> ownedRead = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.experience.get",
                  "arguments": {"experienceId": "%s"}
                }
                """.formatted(experienceId), ownerToken);
        HttpResponse<String> crossUserRead = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.experience.get",
                  "arguments": {"experienceId": "%s"}
                }
                """.formatted(experienceId), otherUserToken);
        HttpResponse<String> unknownTool = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.user.delete",
                  "arguments": {}
                }
                """, ownerToken);
        HttpResponse<String> invalidArguments = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.experience.get",
                  "arguments": {"experienceId": "%s", "userId": "%s"}
                }
                """.formatted(experienceId, UUID.randomUUID()), ownerToken);
        HttpResponse<String> readApproval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.experience.get",
                  "arguments": {"experienceId": "%s"}
                }
                """.formatted(experienceId), ownerToken);
        String createExperienceArguments = """
                {"companyName":"Created by Tool","role":"Backend Developer","description":"Created through an approved tool call."}
                """;
        HttpResponse<String> createExperienceApproval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.experience.create",
                  "arguments": %s
                }
                """.formatted(createExperienceArguments), ownerToken);
        String createExperienceApprovalToken = jsonValue(
                createExperienceApproval.body(),
                "approvalToken"
        );
        HttpResponse<String> createExperienceWithoutApproval = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.experience.create",
                  "arguments": %s
                }
                """.formatted(createExperienceArguments), ownerToken);
        HttpResponse<String> crossUserCreateExperience = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.experience.create",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(createExperienceArguments, createExperienceApprovalToken), otherUserToken);
        HttpResponse<String> createdExperience = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.experience.create",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(createExperienceArguments, createExperienceApprovalToken), ownerToken);

        String ownerUserId = jsonValue(get("/api/v1/users/me", ownerToken).body(), "id");
        HttpResponse<String> resume = post("/api/v1/resumes", """
                {
                  "name": "Tool Approved Resume",
                  "content": "Initial resume content"
                }
                """, ownerToken);
        String resumeId = jsonValue(resume.body(), "id");
        User owner = userRepository.findById(UUID.fromString(ownerUserId)).orElseThrow();
        JobPosting jobPosting = jobPostingRepository.saveAndFlush(JobPosting.create(
                owner,
                "Backend Developer",
                "Tool Corp",
                "Build Java APIs.",
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        ));
        SourceCitation citation = new SourceCitation(
                "C1",
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.fromString(experienceId),
                0,
                "Private owner experience.",
                0.95,
                "nomic-embed-text"
        );
        GroundedResumeDraftLine line = new GroundedResumeDraftLine(
                "Built Java APIs",
                List.of(citation)
        );
        GroundedResumeDraftDocument document = new GroundedResumeDraftDocument(
                line,
                line,
                List.of(line),
                List.of(),
                List.of(line)
        );
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ResumeDraft draft = resumeDraftRepository.saveAndFlush(ResumeDraft.create(
                owner,
                jobPosting,
                "ollama",
                "llama3.1:8b",
                "resume-draft-v1",
                "tool-test-response",
                mapper.valueToTree(document),
                mapper.valueToTree(List.of(citation)),
                1,
                new LlmUsage(10, 20, 30),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        String approvalArguments = """
                {"draftId":"%s","resumeId":"%s"}
                """.formatted(draft.getId(), resumeId);
        HttpResponse<String> approval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.resume-draft.approve",
                  "arguments": %s
                }
                """.formatted(approvalArguments), ownerToken);
        assertThat(approval.statusCode()).as(approval.body()).isEqualTo(201);
        String approvalToken = jsonValue(approval.body(), "approvalToken");
        HttpResponse<String> missingApproval = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.resume-draft.approve",
                  "arguments": %s
                }
                """.formatted(approvalArguments), ownerToken);
        HttpResponse<String> crossUserMutation = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.resume-draft.approve",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(approvalArguments, approvalToken), otherUserToken);
        HttpResponse<String> approved = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.resume-draft.approve",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(approvalArguments, approvalToken), ownerToken);
        HttpResponse<String> replayed = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.resume-draft.approve",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(approvalArguments, approvalToken), ownerToken);

        assertThat(unauthenticatedCatalog.statusCode()).isEqualTo(401);
        assertThat(catalog.statusCode()).isEqualTo(200);
        assertThat(catalog.body()).contains(
                "career.experience.get",
                "career.experience.create",
                "career.job-posting.create",
                "career.project.create",
                "career.resume-draft.approve",
                "inputSchema"
        );
        assertThat(ownedRead.statusCode()).isEqualTo(200);
        assertThat(ownedRead.body()).contains("Private owner experience.");
        assertThat(crossUserRead.statusCode()).isEqualTo(404);
        assertThat(unknownTool.statusCode()).isEqualTo(404);
        assertThat(unknownTool.body()).contains("TOOL_NOT_FOUND");
        assertThat(invalidArguments.statusCode()).isEqualTo(400);
        assertThat(invalidArguments.body()).contains("TOOL_ARGUMENTS_INVALID");
        assertThat(readApproval.statusCode()).isEqualTo(400);
        assertThat(readApproval.body()).contains("TOOL_ARGUMENTS_INVALID");
        assertThat(createExperienceApproval.statusCode()).isEqualTo(201);
        assertThat(createExperienceWithoutApproval.statusCode()).isEqualTo(403);
        assertThat(createExperienceWithoutApproval.body()).contains("TOOL_APPROVAL_REQUIRED");
        assertThat(crossUserCreateExperience.statusCode()).isEqualTo(403);
        assertThat(crossUserCreateExperience.body()).contains("TOOL_APPROVAL_INVALID");
        assertThat(createdExperience.statusCode()).isEqualTo(200);
        assertThat(createdExperience.body()).contains(
                "Created by Tool",
                "Created through an approved tool call",
                "\"mutating\":true"
        );
        assertThat(approval.statusCode()).isEqualTo(201);
        assertThat(missingApproval.statusCode()).isEqualTo(403);
        assertThat(missingApproval.body()).contains("TOOL_APPROVAL_REQUIRED");
        assertThat(crossUserMutation.statusCode()).isEqualTo(403);
        assertThat(crossUserMutation.body()).contains("TOOL_APPROVAL_INVALID");
        assertThat(approved.statusCode()).isEqualTo(200);
        assertThat(approved.body()).contains("Built Java APIs", "versionNumber", "\"mutating\":true");
        assertThat(replayed.statusCode()).isEqualTo(403);
        assertThat(replayed.body()).contains("TOOL_APPROVAL_INVALID");
    }

    @Test
    void projectCreateToolEnforcesDomainValidationAndApproval() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        String invalidArguments = """
                {"name":"Tool Project","role":"Backend Developer","startDate":"2024-01-01","description":"Project description","techStack":"Java","repositoryUrl":"ftp://example.com/repo"}
                """;
        String validArguments = """
                {"name":"Tool Project","role":"Backend Developer","startDate":"2024-01-01","description":"Created through an approved project tool call.","techStack":"Java, PostgreSQL","repositoryUrl":"https://github.com/example/tool-project"}
                """;

        HttpResponse<String> invalidApproval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.project.create",
                  "arguments": %s
                }
                """.formatted(invalidArguments), ownerToken);
        HttpResponse<String> approval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.project.create",
                  "arguments": %s
                }
                """.formatted(validArguments), ownerToken);
        String approvalToken = jsonValue(approval.body(), "approvalToken");
        HttpResponse<String> missingApproval = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.project.create",
                  "arguments": %s
                }
                """.formatted(validArguments), ownerToken);
        HttpResponse<String> crossUserMutation = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.project.create",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(validArguments, approvalToken), otherUserToken);
        HttpResponse<String> created = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.project.create",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(validArguments, approvalToken), ownerToken);

        assertThat(invalidApproval.statusCode()).isEqualTo(400);
        assertThat(invalidApproval.body()).contains("TOOL_ARGUMENTS_INVALID", "repositoryUrl");
        assertThat(approval.statusCode()).as(approval.body()).isEqualTo(201);
        assertThat(missingApproval.statusCode()).isEqualTo(403);
        assertThat(missingApproval.body()).contains("TOOL_APPROVAL_REQUIRED");
        assertThat(crossUserMutation.statusCode()).isEqualTo(403);
        assertThat(crossUserMutation.body()).contains("TOOL_APPROVAL_INVALID");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(200);
        assertThat(created.body()).contains(
                "Tool Project",
                "https://github.com/example/tool-project",
                "Java, PostgreSQL",
                "\"mutating\":true"
        );
    }

    @Test
    void jobPostingCreateToolEnforcesSourceUrlValidationAndApproval() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        String invalidArguments = """
                {"title":"Backend Engineer","companyName":"Tool Corp","description":"Build Java services.","sourceUrl":"ftp://example.com/job"}
                """;
        String validArguments = """
                {"title":"Backend Engineer","companyName":"Tool Corp","description":"Build Java services with PostgreSQL.","sourceUrl":"https://jobs.example.com/backend-engineer"}
                """;

        HttpResponse<String> invalidApproval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.job-posting.create",
                  "arguments": %s
                }
                """.formatted(invalidArguments), ownerToken);
        HttpResponse<String> approval = post("/api/v1/tools/approvals", """
                {
                  "toolName": "career.job-posting.create",
                  "arguments": %s
                }
                """.formatted(validArguments), ownerToken);
        String approvalToken = jsonValue(approval.body(), "approvalToken");
        HttpResponse<String> missingApproval = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.job-posting.create",
                  "arguments": %s
                }
                """.formatted(validArguments), ownerToken);
        HttpResponse<String> crossUserMutation = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.job-posting.create",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(validArguments, approvalToken), otherUserToken);
        HttpResponse<String> created = post("/api/v1/tools/calls", """
                {
                  "toolName": "career.job-posting.create",
                  "arguments": %s,
                  "approvalToken": "%s"
                }
                """.formatted(validArguments, approvalToken), ownerToken);

        assertThat(invalidApproval.statusCode()).isEqualTo(400);
        assertThat(invalidApproval.body()).contains("TOOL_ARGUMENTS_INVALID", "sourceUrl");
        assertThat(approval.statusCode()).as(approval.body()).isEqualTo(201);
        assertThat(missingApproval.statusCode()).isEqualTo(403);
        assertThat(missingApproval.body()).contains("TOOL_APPROVAL_REQUIRED");
        assertThat(crossUserMutation.statusCode()).isEqualTo(403);
        assertThat(crossUserMutation.body()).contains("TOOL_APPROVAL_INVALID");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(200);
        assertThat(created.body()).contains(
                "Backend Engineer",
                "Tool Corp",
                "https://jobs.example.com/backend-engineer",
                "\"mutating\":true"
        );
    }

    @Test
    void jobApplicationContextWorkflowIsBoundedAndUserScoped() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        String jobPostingId = jsonValue(post("/api/v1/job-postings", """
                {
                  "title": "Backend Engineer",
                  "companyName": "Workflow Corp",
                  "description": "Build Java APIs.",
                  "sourceUrl": "https://jobs.example.com/backend"
                }
                """, ownerToken).body(), "id");
        String experienceId = jsonValue(post("/api/v1/experiences", """
                {
                  "companyName": "Owner Corp",
                  "role": "Backend Developer",
                  "startDate": "2022-01-01",
                  "description": "Built Java APIs."
                }
                """, ownerToken).body(), "id");
        String projectId = jsonValue(post("/api/v1/projects", """
                {
                  "name": "Workflow Project",
                  "role": "Backend Developer",
                  "startDate": "2023-01-01",
                  "description": "Built a workflow service.",
                  "techStack": "Java, PostgreSQL"
                }
                """, ownerToken).body(), "id");

        String contextArguments = """
                {"jobPostingId":"%s","experienceIds":["%s"],"projectIds":["%s"]}
                """.formatted(jobPostingId, experienceId, projectId);
        HttpResponse<String> context = post(
                "/api/v1/workflows/job-application-context",
                contextArguments,
                ownerToken
        );
        HttpResponse<String> crossUser = post(
                "/api/v1/workflows/job-application-context",
                contextArguments,
                otherUserToken
        );
        HttpResponse<String> duplicateIds = post("/api/v1/workflows/job-application-context", """
                {"jobPostingId":"%s","experienceIds":["%s","%s"],"projectIds":[]}
                """.formatted(jobPostingId, experienceId, experienceId), ownerToken);
        HttpResponse<String> tooManyProjects = post("/api/v1/workflows/job-application-context", """
                {"jobPostingId":"%s","experienceIds":[],"projectIds":["%s","%s","%s","%s"]}
                """.formatted(
                jobPostingId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        ), ownerToken);

        assertThat(context.statusCode()).as(context.body()).isEqualTo(200);
        assertThat(context.body()).contains(
                "job-application-context-v1",
                "Workflow Corp",
                "Owner Corp",
                "Workflow Project",
                "\"stepsUsed\":3",
                "\"costUnits\":3"
        );
        assertThat(crossUser.statusCode()).isEqualTo(404);
        assertThat(crossUser.body()).contains("RESOURCE_NOT_FOUND");
        assertThat(duplicateIds.statusCode()).isEqualTo(400);
        assertThat(duplicateIds.body()).contains("WORKFLOW_ARGUMENTS_INVALID", "experienceIds");
        assertThat(tooManyProjects.statusCode()).isEqualTo(400);
        assertThat(tooManyProjects.body()).contains("VALIDATION_ERROR", "projectIds");
    }

    @Test
    void vectorSearchIsScopedByUserAndSourceType() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        String ownerUserId = jsonValue(get("/api/v1/users/me", ownerToken).body(), "id");
        String otherUserId = jsonValue(get("/api/v1/users/me", otherUserToken).body(), "id");

        String ownerExperienceId = jsonValue(post("/api/v1/experiences", """
                {
                  "companyName": "Owner Corp",
                  "role": "Backend Developer",
                  "startDate": "2022-01-01",
                  "description": "Build Java APIs for owner services."
                }
                """, ownerToken).body(), "id");
        String otherExperienceId = jsonValue(post("/api/v1/experiences", """
                {
                  "companyName": "Other Corp",
                  "role": "Backend Developer",
                  "startDate": "2022-01-01",
                  "description": "Private other-user experience."
                }
                """, otherUserToken).body(), "id");

        double[] vector = new double[768];
        vector[0] = 1.0;
        Timestamp now = Timestamp.from(Instant.now());
        embeddingChunkRepository.insert(
                UUID.randomUUID(),
                UUID.fromString(ownerUserId),
                UUID.fromString(ownerExperienceId),
                null,
                0,
                "Owner experience source",
                "a".repeat(64),
                vector,
                "test-embedding-model",
                768,
                now,
                now
        );
        embeddingChunkRepository.insert(
                UUID.randomUUID(),
                UUID.fromString(otherUserId),
                UUID.fromString(otherExperienceId),
                null,
                0,
                "Other user source",
                "b".repeat(64),
                vector,
                "test-embedding-model",
                768,
                now,
                now
        );

        List<EmbeddingChunkSearchResult> ownerResults = embeddingChunkRepository.search(
                UUID.fromString(ownerUserId),
                vector,
                5,
                Set.of()
        );
        List<EmbeddingChunkSearchResult> projectResults = embeddingChunkRepository.search(
                UUID.fromString(ownerUserId),
                vector,
                5,
                Set.of(SourceType.PROJECT)
        );

        assertThat(ownerResults).hasSize(1);
        assertThat(ownerResults.get(0).sourceType()).isEqualTo(SourceType.EXPERIENCE);
        assertThat(ownerResults.get(0).sourceId()).isEqualTo(UUID.fromString(ownerExperienceId));
        assertThat(ownerResults.get(0).content()).isEqualTo("Owner experience source");
        assertThat(ownerResults.get(0).similarity()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(projectResults).isEmpty();
    }

    @Test
    void jobAnalysisEnforcesOwnershipAndReturnsProviderConfigurationError() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/job-postings", """
                {
                  "title": "Backend Developer",
                  "companyName": "Example Corp",
                  "description": "Build Java APIs and PostgreSQL services."
                }
                """, ownerToken);
        String jobPostingId = jsonValue(created.body(), "id");

        HttpResponse<String> crossUser = post("/api/v1/ai/analyze-job", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId), otherUserToken);
        HttpResponse<String> missingProvider = post("/api/v1/ai/analyze-job", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId), ownerToken);
        HttpResponse<String> unauthenticated = post("/api/v1/ai/analyze-job", """
                {"jobPostingId":"%s"}
                """.formatted(jobPostingId));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(crossUser.statusCode()).isEqualTo(404);
        assertThat(missingProvider.statusCode()).isEqualTo(503);
        assertThat(missingProvider.body()).contains("AI_PROVIDER_UNAVAILABLE");
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
    }

    @Test
    void signupLoginAndCurrentUserFlowWorks() throws Exception {
        String email = uniqueEmail();
        String password = "correct-password-123";

        HttpResponse<String> signup = post("/api/v1/auth/signup", """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "Test User"
                }
                """.formatted(email, password));

        assertThat(signup.statusCode()).isEqualTo(201);
        assertThat(signup.body()).contains(email, "Test User");

        HttpResponse<String> login = post("/api/v1/auth/login", """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password));

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"tokenType\":\"Bearer\"");

        String accessToken = jsonValue(login.body(), "accessToken");
        HttpResponse<String> currentUser = get("/api/v1/users/me", accessToken);

        assertThat(currentUser.statusCode()).isEqualTo(200);
        assertThat(currentUser.body()).contains(email, "Test User");
    }

    @Test
    void duplicateSignupReturnsConflict() throws Exception {
        String email = uniqueEmail();
        String body = """
                {
                  "email": "%s",
                  "password": "correct-password-123",
                  "displayName": "Test User"
                }
                """.formatted(email);

        assertThat(post("/api/v1/auth/signup", body).statusCode()).isEqualTo(201);

        HttpResponse<String> duplicate = post("/api/v1/auth/signup", body);

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void invalidCredentialsAndProtectedAccessAreRejected() throws Exception {
        String email = uniqueEmail();
        post("/api/v1/auth/signup", """
                {
                  "email": "%s",
                  "password": "correct-password-123",
                  "displayName": "Test User"
                }
                """.formatted(email));

        HttpResponse<String> invalidLogin = post("/api/v1/auth/login", """
                {
                  "email": "%s",
                  "password": "wrong-password-123"
                }
                """.formatted(email));
        HttpResponse<String> unauthenticated = get("/api/v1/users/me", null);

        assertThat(invalidLogin.statusCode()).isEqualTo(401);
        assertThat(invalidLogin.body()).contains("AUTHENTICATION_FAILED");
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
        assertThat(unauthenticated.body()).contains("AUTHENTICATION_REQUIRED");
    }

    @Test
    void refreshTokenRotatesAndLogoutRevokesSession() throws Exception {
        String email = uniqueEmail();
        String password = "correct-password-123";
        post("/api/v1/auth/signup", """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "Test User"
                }
                """.formatted(email, password));

        HttpResponse<String> login = post("/api/v1/auth/login", """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password));
        String accessToken = jsonValue(login.body(), "accessToken");
        String originalRefreshToken = jsonValue(login.body(), "refreshToken");

        HttpResponse<String> refresh = post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(originalRefreshToken));
        String rotatedRefreshToken = jsonValue(refresh.body(), "refreshToken");

        HttpResponse<String> reusedOriginal = post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(originalRefreshToken));
        HttpResponse<String> logout = post("/api/v1/auth/logout", """
                {"refreshToken":"%s"}
                """.formatted(rotatedRefreshToken));
        HttpResponse<String> refreshAfterLogout = post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(rotatedRefreshToken));
        HttpResponse<String> currentUserWithExistingAccessToken = get("/api/v1/users/me", accessToken);

        assertThat(refresh.statusCode()).isEqualTo(200);
        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);
        assertThat(reusedOriginal.statusCode()).isEqualTo(401);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(refreshAfterLogout.statusCode()).isEqualTo(401);
        assertThat(currentUserWithExistingAccessToken.statusCode()).isEqualTo(200);
    }

    @Test
    void invalidSignupRequestReturnsValidationError() throws Exception {
        HttpResponse<String> response = post("/api/v1/auth/signup", """
                {
                  "email": "not-an-email",
                  "password": "short",
                  "displayName": ""
                }
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("VALIDATION_ERROR", "email", "password");
    }

    @Test
    void experienceCrudAndPaginationWork() throws Exception {
        String accessToken = accessTokenForNewUser();
        String firstBody = """
                {
                  "companyName": "First Company",
                  "role": "Backend Developer",
                  "startDate": "2022-01-01",
                  "endDate": "2023-01-31",
                  "description": "Built APIs and improved database queries."
                }
                """;
        String secondBody = """
                {
                  "companyName": "Second Company",
                  "role": "Senior Backend Developer",
                  "startDate": "2023-02-01",
                  "endDate": null,
                  "description": "Designed a Spring Boot platform."
                }
                """;

        HttpResponse<String> first = post("/api/v1/experiences", firstBody, accessToken);
        HttpResponse<String> second = post("/api/v1/experiences", secondBody, accessToken);
        String firstId = jsonValue(first.body(), "id");

        HttpResponse<String> page = get("/api/v1/experiences?page=0&size=1", accessToken);
        HttpResponse<String> retrieved = get("/api/v1/experiences/" + firstId, accessToken);
        HttpResponse<String> updated = put("/api/v1/experiences/" + firstId, """
                {
                  "companyName": "First Company Updated",
                  "role": "Backend Developer",
                  "startDate": "2022-01-01",
                  "endDate": "2023-02-15",
                  "description": "Built APIs and improved database queries at scale."
                }
                """, accessToken);
        HttpResponse<String> deleted = delete("/api/v1/experiences/" + firstId, accessToken);
        HttpResponse<String> afterDelete = get("/api/v1/experiences/" + firstId, accessToken);
        HttpResponse<String> remaining = get("/api/v1/experiences?page=0&size=20", accessToken);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("\"size\":1", "\"totalElements\":2", "\"totalPages\":2");
        assertThat(retrieved.statusCode()).isEqualTo(200);
        assertThat(retrieved.body()).contains("First Company", "Backend Developer");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("First Company Updated");
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(afterDelete.statusCode()).isEqualTo(404);
        assertThat(afterDelete.body()).contains("RESOURCE_NOT_FOUND");
        assertThat(remaining.body()).contains("\"totalElements\":1");
    }

    @Test
    void projectCrudAndSoftDeleteWork() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/projects", """
                {
                  "name": "Career Platform",
                  "role": "Backend Developer",
                  "startDate": "2024-01-01",
                  "endDate": null,
                  "description": "Built a modular monolith career platform.",
                  "techStack": "Java, Spring Boot, PostgreSQL",
                  "repositoryUrl": "https://github.com/example/career-platform"
                }
                """, accessToken);
        String projectId = jsonValue(created.body(), "id");

        HttpResponse<String> retrieved = get("/api/v1/projects/" + projectId, accessToken);
        HttpResponse<String> updated = put("/api/v1/projects/" + projectId, """
                {
                  "name": "Career Platform Updated",
                  "role": "Backend Developer",
                  "startDate": "2024-01-01",
                  "endDate": "2025-01-01",
                  "description": "Built and tested a modular monolith career platform.",
                  "techStack": "Java, Spring Boot, PostgreSQL, Testcontainers",
                  "repositoryUrl": ""
                }
                """, accessToken);
        HttpResponse<String> page = get("/api/v1/projects?page=0&size=20", accessToken);
        HttpResponse<String> deleted = delete("/api/v1/projects/" + projectId, accessToken);
        HttpResponse<String> afterDelete = get("/api/v1/projects/" + projectId, accessToken);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(retrieved.statusCode()).isEqualTo(200);
        assertThat(retrieved.body()).contains("Career Platform", "Java, Spring Boot, PostgreSQL");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Career Platform Updated", "Testcontainers");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("\"totalElements\":1");
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(afterDelete.statusCode()).isEqualTo(404);
    }

    @Test
    void jobPostingCrudPaginationAndSoftDeleteWork() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/job-postings", """
                {
                  "title": "Backend Developer",
                  "companyName": "Career Platform Inc.",
                  "description": "Build reliable Java and Spring Boot services.",
                  "sourceUrl": "https://example.com/jobs/backend"
                }
                """, accessToken);
        String jobPostingId = jsonValue(created.body(), "id");

        HttpResponse<String> retrieved = get("/api/v1/job-postings/" + jobPostingId, accessToken);
        HttpResponse<String> listed = get("/api/v1/job-postings?page=0&size=1", accessToken);
        HttpResponse<String> updated = put("/api/v1/job-postings/" + jobPostingId, """
                {
                  "title": "Senior Backend Developer",
                  "companyName": "Career Platform Inc.",
                  "description": "Design and operate reliable Java, Spring Boot, and PostgreSQL services.",
                  "sourceUrl": ""
                }
                """, accessToken);
        HttpResponse<String> deleted = delete("/api/v1/job-postings/" + jobPostingId, accessToken);
        HttpResponse<String> afterDelete = get("/api/v1/job-postings/" + jobPostingId, accessToken);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("Backend Developer", "Career Platform Inc.");
        assertThat(retrieved.statusCode()).isEqualTo(200);
        assertThat(retrieved.body()).contains("Build reliable Java", "https://example.com/jobs/backend");
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(listed.body()).contains("\"size\":1", "\"totalElements\":1");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Senior Backend Developer", "PostgreSQL");
        assertThat(updated.body()).contains("\"sourceUrl\":null");
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(afterDelete.statusCode()).isEqualTo(404);
    }

    @Test
    void jobPostingOwnershipAndValidationAreEnforced() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/job-postings", """
                {
                  "title": "Private Job",
                  "companyName": "Private Company",
                  "description": "Private job description.",
                  "sourceUrl": null
                }
                """, ownerToken);
        String jobPostingId = jsonValue(created.body(), "id");

        HttpResponse<String> otherGet = get("/api/v1/job-postings/" + jobPostingId, otherUserToken);
        HttpResponse<String> otherUpdate = put("/api/v1/job-postings/" + jobPostingId, """
                {
                  "title": "Attempted Update",
                  "companyName": "Other User",
                  "description": "Must not update another user's job posting.",
                  "sourceUrl": null
                }
                """, otherUserToken);
        HttpResponse<String> otherDelete = delete("/api/v1/job-postings/" + jobPostingId, otherUserToken);
        HttpResponse<String> otherList = get("/api/v1/job-postings?page=0&size=20", otherUserToken);
        HttpResponse<String> invalid = post("/api/v1/job-postings", """
                {
                  "title": "",
                  "companyName": "Company",
                  "description": "Description",
                  "sourceUrl": "ftp://example.com/job"
                }
                """, ownerToken);
        HttpResponse<String> missing = get("/api/v1/job-postings/" + UUID.randomUUID(), ownerToken);
        HttpResponse<String> unauthenticated = get("/api/v1/job-postings", null);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(otherGet.statusCode()).isEqualTo(404);
        assertThat(otherUpdate.statusCode()).isEqualTo(404);
        assertThat(otherDelete.statusCode()).isEqualTo(404);
        assertThat(otherList.statusCode()).isEqualTo(200);
        assertThat(otherList.body()).contains("\"totalElements\":0");
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("VALIDATION_ERROR", "title", "sourceUrl");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
    }

    @Test
    void jobPostingImportPreviewRequiresAuthenticationAndValidHttpUrl() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> unauthenticated = post("/api/v1/job-postings/import-preview", """
                {"sourceUrl":"https://example.com/jobs/backend"}
                """);
        HttpResponse<String> malformed = post("/api/v1/job-postings/import-preview", """
                {"sourceUrl":"ftp://example.com/jobs/backend"}
                """, accessToken);
        HttpResponse<String> privateHost = post("/api/v1/job-postings/import-preview", """
                {"sourceUrl":"http://localhost:8080/jobs/backend"}
                """, accessToken);

        assertThat(unauthenticated.statusCode()).isEqualTo(401);
        assertThat(malformed.statusCode()).isEqualTo(400);
        assertThat(malformed.body()).contains("VALIDATION_ERROR", "sourceUrl");
        assertThat(privateHost.statusCode()).isEqualTo(400);
        assertThat(privateHost.body()).contains("INVALID_ARGUMENT");
    }

    @Test
    void careerResourcesAreIsolatedByAuthenticatedUser() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/experiences", """
                {
                  "companyName": "Private Company",
                  "role": "Backend Developer",
                  "startDate": "2024-01-01",
                  "endDate": null,
                  "description": "Private experience."
                }
                """, ownerToken);
        String experienceId = jsonValue(created.body(), "id");

        HttpResponse<String> otherGet = get("/api/v1/experiences/" + experienceId, otherUserToken);
        HttpResponse<String> otherUpdate = put("/api/v1/experiences/" + experienceId, """
                {
                  "companyName": "Attempted Update",
                  "role": "Backend Developer",
                  "startDate": "2024-01-01",
                  "endDate": null,
                  "description": "Must not update another user's resource."
                }
                """, otherUserToken);
        HttpResponse<String> otherDelete = delete("/api/v1/experiences/" + experienceId, otherUserToken);
        HttpResponse<String> otherList = get("/api/v1/experiences?page=0&size=20", otherUserToken);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(otherGet.statusCode()).isEqualTo(404);
        assertThat(otherUpdate.statusCode()).isEqualTo(404);
        assertThat(otherDelete.statusCode()).isEqualTo(404);
        assertThat(otherList.statusCode()).isEqualTo(200);
        assertThat(otherList.body()).contains("\"totalElements\":0");
    }

    @Test
    void careerValidationAndPaginationBoundariesAreRejected() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> invalidExperience = post("/api/v1/experiences", """
                {
                  "companyName": "",
                  "role": "Backend Developer",
                  "startDate": "2024-02-01",
                  "endDate": "2024-01-01",
                  "description": ""
                }
                """, accessToken);
        HttpResponse<String> invalidProject = post("/api/v1/projects", """
                {
                  "name": "Project",
                  "role": "Developer",
                  "startDate": "2024-01-01",
                  "endDate": null,
                  "description": "Description",
                  "techStack": "Java",
                  "repositoryUrl": "ftp://example.com/repository"
                }
                """, accessToken);
        HttpResponse<String> negativePage = get("/api/v1/experiences?page=-1&size=20", accessToken);
        HttpResponse<String> oversizedPage = get("/api/v1/projects?page=0&size=101", accessToken);
        HttpResponse<String> malformedId = get("/api/v1/experiences/not-a-uuid", accessToken);
        HttpResponse<String> unauthenticated = get("/api/v1/experiences", null);

        assertThat(invalidExperience.statusCode()).isEqualTo(400);
        assertThat(invalidExperience.body()).contains("VALIDATION_ERROR");
        assertThat(invalidProject.statusCode()).isEqualTo(400);
        assertThat(invalidProject.body()).contains("VALIDATION_ERROR", "repositoryUrl");
        assertThat(negativePage.statusCode()).isEqualTo(400);
        assertThat(oversizedPage.statusCode()).isEqualTo(400);
        assertThat(malformedId.statusCode()).isEqualTo(400);
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
    }

    @Test
    void resumeCreateListAndRenameWork() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/resumes", """
                {
                  "name": "Backend Resume",
                  "content": "# Backend Developer\\n\\nJava and Spring Boot experience."
                }
                """, accessToken);
        String resumeId = jsonValue(created.body(), "id");

        HttpResponse<String> retrieved = get("/api/v1/resumes/" + resumeId, accessToken);
        HttpResponse<String> listed = get("/api/v1/resumes?page=0&size=20", accessToken);
        HttpResponse<String> renamed = put("/api/v1/resumes/" + resumeId, """
                {"name":"Backend Resume Updated"}
                """, accessToken);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("Backend Resume", "\"latestVersionNumber\":1");
        assertThat(retrieved.statusCode()).isEqualTo(200);
        assertThat(retrieved.body()).contains("Backend Resume", "\"latestVersionNumber\":1");
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(listed.body()).contains("\"totalElements\":1", "Backend Resume");
        assertThat(renamed.statusCode()).isEqualTo(200);
        assertThat(renamed.body()).contains("Backend Resume Updated", "\"latestVersionNumber\":1");
    }

    @Test
    void resumeVersionsAreCreatedAsImmutableSnapshots() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> resume = post("/api/v1/resumes", """
                {
                  "name": "Versioned Resume",
                  "content": "Initial resume content"
                }
                """, accessToken);
        String resumeId = jsonValue(resume.body(), "id");

        HttpResponse<String> versionsBefore = get("/api/v1/resumes/" + resumeId + "/versions?page=0&size=20", accessToken);
        String firstVersionId = jsonValue(versionsBefore.body(), "id");
        HttpResponse<String> firstVersion = get(
                "/api/v1/resumes/" + resumeId + "/versions/" + firstVersionId,
                accessToken
        );
        HttpResponse<String> secondVersion = post("/api/v1/resumes/" + resumeId + "/versions", """
                {"content":"Improved resume content"}
                """, accessToken);
        String secondVersionId = jsonValue(secondVersion.body(), "id");
        HttpResponse<String> firstVersionAfterNewVersion = get(
                "/api/v1/resumes/" + resumeId + "/versions/" + firstVersionId,
                accessToken
        );
        HttpResponse<String> versionsAfter = get("/api/v1/resumes/" + resumeId + "/versions?page=0&size=1", accessToken);
        HttpResponse<String> attemptedVersionUpdate = put(
                "/api/v1/resumes/" + resumeId + "/versions/" + secondVersionId,
                "{\"content\":\"Must not mutate an existing version\"}",
                accessToken
        );

        assertThat(versionsBefore.statusCode()).isEqualTo(200);
        assertThat(versionsBefore.body()).contains("\"totalElements\":1", "\"versionNumber\":1");
        assertThat(firstVersion.statusCode()).isEqualTo(200);
        assertThat(firstVersion.body()).contains("Initial resume content", "\"versionNumber\":1");
        assertThat(secondVersion.statusCode()).isEqualTo(201);
        assertThat(secondVersion.body()).contains("Improved resume content", "\"versionNumber\":2");
        assertThat(firstVersionAfterNewVersion.statusCode()).isEqualTo(200);
        assertThat(firstVersionAfterNewVersion.body()).contains("Initial resume content")
                .doesNotContain("Improved resume content");
        assertThat(versionsAfter.statusCode()).isEqualTo(200);
        assertThat(versionsAfter.body()).contains("\"size\":1", "\"totalElements\":2", "\"totalPages\":2");
        assertThat(attemptedVersionUpdate.statusCode()).isEqualTo(405);
    }

    @Test
    void jobKoreaExportAuditRecordsOnlyOwnedVersionMetadata() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> resume = post("/api/v1/resumes", """
                {
                  "name": "JobKorea Export Resume",
                  "content": "Reviewed immutable resume content"
                }
                """, ownerToken);
        String resumeId = jsonValue(resume.body(), "id");
        HttpResponse<String> versions = get(
                "/api/v1/resumes/" + resumeId + "/versions?page=0&size=20",
                ownerToken
        );
        String versionId = jsonValue(versions.body(), "id");
        String successBody = """
                {
                  "resumeVersionId":"%s",
                  "templateVersion":"JOBKOREA_STANDARD_V2",
                  "result":"DOWNLOAD_TRIGGERED",
                  "fileSizeBytes":123456
                }
                """.formatted(versionId);

        HttpResponse<String> crossUser = post(
                "/api/v1/jobkorea/export-audits",
                successBody,
                otherUserToken
        );
        HttpResponse<String> unauthenticated = post(
                "/api/v1/jobkorea/export-audits",
                successBody,
                null
        );
        HttpResponse<String> missingFileSize = post(
                "/api/v1/jobkorea/export-audits",
                """
                {
                  "resumeVersionId":"%s",
                  "templateVersion":"JOBKOREA_STANDARD_V2",
                  "result":"DOWNLOAD_TRIGGERED"
                }
                """.formatted(versionId),
                ownerToken
        );
        HttpResponse<String> recorded = post(
                "/api/v1/jobkorea/export-audits",
                successBody,
                ownerToken
        );
        HttpResponse<String> failed = post(
                "/api/v1/jobkorea/export-audits",
                """
                {
                  "resumeVersionId":"%s",
                  "templateVersion":"JOBKOREA_STANDARD_V2",
                  "result":"GENERATION_FAILED"
                }
                """.formatted(versionId),
                ownerToken
        );

        assertThat(resume.statusCode()).isEqualTo(201);
        assertThat(crossUser.statusCode()).isEqualTo(404);
        assertThat(unauthenticated.statusCode()).isEqualTo(401);
        assertThat(missingFileSize.statusCode()).isEqualTo(400);
        assertThat(recorded.statusCode()).isEqualTo(201);
        assertThat(recorded.body())
                .contains(versionId, "JOBKOREA_STANDARD_V2", "DOWNLOAD_TRIGGERED", "123456")
                .doesNotContain("Reviewed immutable resume content");
        assertThat(failed.statusCode()).isEqualTo(201);
        assertThat(failed.body()).contains(versionId, "GENERATION_FAILED", "\"fileSizeBytes\":null");
    }

    @Test
    void resumeAndVersionOwnershipAndSoftDeleteAreEnforced() throws Exception {
        String ownerToken = accessTokenForNewUser();
        String otherUserToken = accessTokenForNewUser();
        HttpResponse<String> created = post("/api/v1/resumes", """
                {
                  "name": "Private Resume",
                  "content": "Private resume content"
                }
                """, ownerToken);
        String resumeId = jsonValue(created.body(), "id");

        HttpResponse<String> otherGet = get("/api/v1/resumes/" + resumeId, otherUserToken);
        HttpResponse<String> otherVersions = get(
                "/api/v1/resumes/" + resumeId + "/versions?page=0&size=20",
                otherUserToken
        );
        HttpResponse<String> otherCreateVersion = post(
                "/api/v1/resumes/" + resumeId + "/versions",
                "{\"content\":\"Must not create\"}",
                otherUserToken
        );
        HttpResponse<String> deleted = delete("/api/v1/resumes/" + resumeId, ownerToken);
        HttpResponse<String> ownerGetAfterDelete = get("/api/v1/resumes/" + resumeId, ownerToken);
        HttpResponse<String> ownerVersionsAfterDelete = get(
                "/api/v1/resumes/" + resumeId + "/versions?page=0&size=20",
                ownerToken
        );
        HttpResponse<String> ownerListAfterDelete = get("/api/v1/resumes?page=0&size=20", ownerToken);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(otherGet.statusCode()).isEqualTo(404);
        assertThat(otherVersions.statusCode()).isEqualTo(404);
        assertThat(otherCreateVersion.statusCode()).isEqualTo(404);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(ownerGetAfterDelete.statusCode()).isEqualTo(404);
        assertThat(ownerVersionsAfterDelete.statusCode()).isEqualTo(404);
        assertThat(ownerListAfterDelete.body()).contains("\"totalElements\":0");
    }

    @Test
    void resumeValidationAndPaginationBoundariesAreRejected() throws Exception {
        String accessToken = accessTokenForNewUser();
        HttpResponse<String> invalidCreate = post("/api/v1/resumes", """
                {"name":"","content":""}
                """, accessToken);
        HttpResponse<String> invalidVersion = post("/api/v1/resumes/not-a-uuid/versions", """
                {"content":"Content"}
                """, accessToken);
        HttpResponse<String> oversizedPage = get("/api/v1/resumes?page=0&size=101", accessToken);
        HttpResponse<String> malformedId = get("/api/v1/resumes/not-a-uuid", accessToken);

        assertThat(invalidCreate.statusCode()).isEqualTo(400);
        assertThat(invalidCreate.body()).contains("VALIDATION_ERROR", "name", "content");
        assertThat(invalidVersion.statusCode()).isEqualTo(400);
        assertThat(invalidVersion.body()).contains("INVALID_PARAMETER", "resumeId");
        assertThat(oversizedPage.statusCode()).isEqualTo(400);
        assertThat(malformedId.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return post(path, body, null);
    }

    private HttpResponse<String> post(String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .GET();
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String uniqueEmail() {
        return "step4-" + UUID.randomUUID() + "@example.com";
    }

    private String accessTokenForNewUser() throws Exception {
        String email = uniqueEmail();
        String password = "correct-password-123";
        HttpResponse<String> signup = post("/api/v1/auth/signup", """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "Step 4 User"
                }
                """.formatted(email, password));
        assertThat(signup.statusCode()).isEqualTo(201);

        HttpResponse<String> login = post("/api/v1/auth/login", """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password));
        assertThat(login.statusCode()).isEqualTo(200);
        return jsonValue(login.body(), "accessToken");
    }

    private String jsonValue(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(json);
        assertThat(matcher.find()).as("JSON field %s", field).isTrue();
        return matcher.group(1);
    }
}
