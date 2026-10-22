package com.aicareer.platform.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import com.aicareer.platform.identity.domain.EmailVerificationCode;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.EmailVerificationCodeRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.identity.infrastructure.security.EmailVerificationCodeHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository codeRepository;
    private final EmailVerificationMessageSender messageSender;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailVerificationCodeHasher codeHasher;
    private final EmailVerificationProperties properties;
    private final EmailVerificationRateLimiter rateLimiter;
    private final Clock clock;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationCodeRepository codeRepository,
            EmailVerificationMessageSender messageSender,
            VerificationCodeGenerator codeGenerator,
            EmailVerificationCodeHasher codeHasher,
            EmailVerificationProperties properties,
            EmailVerificationRateLimiter rateLimiter,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.messageSender = messageSender;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    public Instant issueInitialCode(User user) {
        if (!properties.enabled() || !user.isEmailVerificationPending()) {
            throw new IllegalStateException("Email verification is not enabled for this account");
        }
        return issueCode(user, clock.instant());
    }

    @Transactional(noRollbackFor = EmailVerificationException.class)
    public void verify(String rawEmail, String rawCode) {
        if (!properties.enabled()) {
            throw invalidCode();
        }
        Instant now = clock.instant();
        User user = userRepository.findByEmailAndDeletedAtIsNullForUpdate(normalizeEmail(rawEmail))
                .filter(User::isEmailVerificationPending)
                .orElseThrow(this::invalidCode);
        EmailVerificationCode verificationCode = codeRepository
                .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(this::invalidCode);

        if (!verificationCode.canAttempt(now)) {
            verificationCode.consume(now);
            throw invalidCode();
        }
        if (!codeHasher.matches(rawCode, verificationCode.getCodeHash())) {
            verificationCode.recordFailedAttempt(now);
            throw invalidCode();
        }

        verificationCode.consume(now);
        user.verifyEmail(now);
    }

    @Transactional
    public void resend(String rawEmail) {
        if (!properties.enabled()) {
            return;
        }
        Instant now = clock.instant();
        userRepository.findByEmailAndDeletedAtIsNullForUpdate(normalizeEmail(rawEmail))
                .filter(User::isEmailVerificationPending)
                .ifPresent(user -> resendFor(user, now));
    }

    private void resendFor(User user, Instant now) {
        EmailVerificationCode currentCode = codeRepository
                .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElse(null);
        if (currentCode != null && !currentCode.canResendAfter(now, properties.resendCooldown())) {
            return;
        }
        if (currentCode != null) {
            currentCode.consume(now);
        }
        issueCode(user, now);
    }

    private Instant issueCode(User user, Instant now) {
        rateLimiter.checkDelivery();
        String rawCode = codeGenerator.generate();
        Instant expiresAt = now.plus(properties.codeTtl());
        codeRepository.saveAndFlush(EmailVerificationCode.issue(
                user,
                codeHasher.hash(rawCode),
                expiresAt,
                properties.maxAttempts(),
                now
        ));
        messageSender.sendVerificationCode(user.getEmail(), rawCode, properties.codeTtl());
        return expiresAt;
    }

    private EmailVerificationException invalidCode() {
        return new EmailVerificationException(
                EmailVerificationException.Kind.INVALID_CODE,
                "The email verification code is invalid or expired"
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
