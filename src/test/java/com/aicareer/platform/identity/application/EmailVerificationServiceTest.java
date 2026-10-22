package com.aicareer.platform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import com.aicareer.platform.identity.domain.EmailVerificationCode;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.domain.UserStatus;
import com.aicareer.platform.identity.infrastructure.persistence.EmailVerificationCodeRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.identity.infrastructure.security.EmailVerificationCodeHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T07:00:00Z");
    private static final String CODE = "123456";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailVerificationCodeRepository codeRepository = mock(EmailVerificationCodeRepository.class);
    private final EmailVerificationMessageSender messageSender = mock(EmailVerificationMessageSender.class);
    private final EmailVerificationProperties properties = properties();
    private final EmailVerificationCodeHasher codeHasher = new EmailVerificationCodeHasher(properties);
    private final EmailVerificationRateLimiter rateLimiter = mock(EmailVerificationRateLimiter.class);
    private final EmailVerificationService service = new EmailVerificationService(
            userRepository,
            codeRepository,
            messageSender,
            () -> CODE,
            codeHasher,
            properties,
            rateLimiter,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createPendingEmailVerification("member@example.com", "hash", "Member", NOW);
        when(codeRepository.saveAndFlush(any(EmailVerificationCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issuesOnlyHashedCodeAndActivatesUserAfterCorrectVerification() {
        Instant expiresAt = service.issueInitialCode(user);
        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeRepository).saveAndFlush(codeCaptor.capture());
        EmailVerificationCode storedCode = codeCaptor.getValue();

        assertThat(expiresAt).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(storedCode.getCodeHash()).isNotEqualTo(CODE).hasSize(64);
        verify(messageSender).sendVerificationCode("member@example.com", CODE, Duration.ofMinutes(5));

        when(userRepository.findByEmailAndDeletedAtIsNullForUpdate("member@example.com"))
                .thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(storedCode));

        service.verify(" MEMBER@EXAMPLE.COM ", CODE);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsCodeAfterFiveFailedAttempts() {
        service.issueInitialCode(user);
        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeRepository).saveAndFlush(codeCaptor.capture());
        EmailVerificationCode storedCode = codeCaptor.getValue();
        when(userRepository.findByEmailAndDeletedAtIsNullForUpdate("member@example.com"))
                .thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(storedCode));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.verify("member@example.com", "000000"))
                    .isInstanceOf(EmailVerificationException.class);
        }
        assertThatThrownBy(() -> service.verify("member@example.com", CODE))
                .isInstanceOf(EmailVerificationException.class);
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_EMAIL_VERIFICATION);
    }

    @Test
    void resendDoesNotRevealUnknownEmailAndHonorsCooldown() {
        service.resend("unknown@example.com");
        verify(messageSender, never()).sendVerificationCode(any(), any(), any());

        when(userRepository.findByEmailAndDeletedAtIsNullForUpdate("member@example.com"))
                .thenReturn(Optional.of(user));
        EmailVerificationCode currentCode = EmailVerificationCode.issue(
                user,
                codeHasher.hash(CODE),
                NOW.plus(Duration.ofMinutes(5)),
                5,
                NOW
        );
        when(codeRepository.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(currentCode));

        service.resend("member@example.com");

        verify(messageSender, never()).sendVerificationCode(any(), any(), any());
    }

    private static EmailVerificationProperties properties() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        return new EmailVerificationProperties(
                true,
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                5,
                secret,
                "smtp.email.ap-seoul-1.oci.oraclecloud.com",
                465,
                "smtp-user",
                "smtp-password",
                true,
                false,
                "noreply@example.com",
                "Career Note"
        );
    }
}
