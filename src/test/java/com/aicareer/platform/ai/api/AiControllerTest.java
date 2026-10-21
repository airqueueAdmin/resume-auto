package com.aicareer.platform.ai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.aicareer.platform.ai.application.JobAnalysisService;
import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.RagMatchingService;
import com.aicareer.platform.ai.application.ResumeDraftApprovalService;
import com.aicareer.platform.ai.application.ResumeDraftService;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerOutcome;
import com.aicareer.platform.ai.application.SelfIntroductionAnswerService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.Jwt;

class AiControllerTest {

    @ParameterizedTest
    @ValueSource(strings = {"local", "docker", "production", "test", "default-local"})
    void showsAnswerDiagnosticsOnlyForTheBackendLocalProfile(String profile) {
        MockEnvironment environment = new MockEnvironment();
        if (profile.equals("default-local")) {
            environment.setDefaultProfiles("local");
        } else {
            environment.setActiveProfiles(profile);
        }
        UUID userId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        SelfIntroductionAnswerService answerService = mock(SelfIntroductionAnswerService.class);
        when(answerService.generate(userId, resumeId, questionId)).thenReturn(new SelfIntroductionAnswerOutcome(
                "검토할 자기소개서 초안입니다.", List.of(), "ollama", "llama3.1:8b",
                "self-introduction-v5", new LlmUsage(10, 20, 30), 7
        ));
        AiController controller = new AiController(
                mock(JobAnalysisService.class), mock(RagMatchingService.class), mock(ResumeDraftService.class),
                mock(ResumeDraftApprovalService.class), answerService, environment
        );

        SelfIntroductionAnswerResponse response = controller.generateSelfIntroductionAnswer(
                jwt, new GenerateSelfIntroductionAnswerRequest(resumeId, questionId)
        );

        assertThat(response.diagnosticsVisible()).isEqualTo(profile.equals("local") || profile.equals("default-local"));
        assertThat(response.answer()).isEqualTo("검토할 자기소개서 초안입니다.");
        verify(answerService).generate(userId, resumeId, questionId);
    }
}
