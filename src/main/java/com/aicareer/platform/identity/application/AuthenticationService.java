package com.aicareer.platform.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.aicareer.platform.identity.domain.RefreshSession;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.domain.UserConsent;
import com.aicareer.platform.identity.infrastructure.persistence.RefreshSessionRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserConsentRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.identity.infrastructure.security.JwtProperties;
import com.aicareer.platform.identity.infrastructure.security.JwtTokenService;
import com.aicareer.platform.identity.infrastructure.security.TokenHashing;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final UserConsentRepository userConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SignupPolicyProperties signupPolicyProperties;
    private final EmailVerificationProperties emailVerificationProperties;
    private final EmailVerificationService emailVerificationService;

    public AuthenticationService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            UserConsentRepository userConsentRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            Clock clock,
            SignupPolicyProperties signupPolicyProperties,
            EmailVerificationProperties emailVerificationProperties,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.userConsentRepository = userConsentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.signupPolicyProperties = signupPolicyProperties;
        this.emailVerificationProperties = emailVerificationProperties;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegistrationResult register(String rawEmail, String rawPassword, String displayName) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.findByEmailAndDeletedAtIsNull(email)
                .filter(User::isEmailVerificationPending)
                .isPresent()) {
            throw new EmailVerificationException(
                    EmailVerificationException.Kind.REQUIRED,
                    "Email verification is required for this account"
            );
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        Instant now = clock.instant();
        User user = emailVerificationProperties.enabled()
                ? User.createPendingEmailVerification(
                        email,
                        passwordEncoder.encode(rawPassword),
                        displayName.trim(),
                        now
                )
                : User.create(
                        email,
                        passwordEncoder.encode(rawPassword),
                        displayName.trim(),
                        now
                );
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }
        userConsentRepository.save(UserConsent.accept(
                user,
                signupPolicyProperties.termsVersion(),
                signupPolicyProperties.privacyVersion(),
                now
        ));
        Instant verificationExpiresAt = emailVerificationProperties.enabled()
                ? emailVerificationService.issueInitialCode(user)
                : null;
        return new RegistrationResult(user, emailVerificationProperties.enabled(), verificationExpiresAt);
    }

    @Transactional
    public AuthenticationResult login(String rawEmail, String rawPassword) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(rawEmail))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.isEmailVerificationPending()) {
            throw new EmailVerificationException(
                    EmailVerificationException.Kind.REQUIRED,
                    "Email verification is required for this account"
            );
        }
        if (!user.canAuthenticate()) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthenticationResult refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshSession currentSession = refreshSessionRepository
                .findByTokenHash(TokenHashing.sha256(rawRefreshToken))
                .filter(session -> session.isActive(now))
                .orElseThrow(InvalidRefreshTokenException::new);

        User user = currentSession.getUser();
        if (!user.canAuthenticate()) {
            throw new InvalidRefreshTokenException();
        }

        currentSession.rotate(now);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshSessionRepository.findByTokenHash(TokenHashing.sha256(rawRefreshToken))
                .filter(session -> session.isActive(clock.instant()))
                .ifPresent(session -> session.rotate(clock.instant()));
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNullForUpdate(userId)
                .orElseThrow(UserNotFoundException::new);

        Instant now = clock.instant();
        refreshSessionRepository.deleteAllByUser_Id(userId);
        user.delete(now);
    }

    private AuthenticationResult issueTokens(User user) {
        Instant now = clock.instant();
        String refreshToken = jwtTokenService.createRefreshToken();
        RefreshSession refreshSession = RefreshSession.create(
                user,
                TokenHashing.sha256(refreshToken),
                now.plus(jwtProperties.refreshTokenTtl()),
                now
        );
        refreshSessionRepository.save(refreshSession);

        return new AuthenticationResult(
                user,
                jwtTokenService.createAccessToken(user),
                refreshToken,
                jwtProperties.accessTokenTtl(),
                jwtProperties.refreshTokenTtl()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
