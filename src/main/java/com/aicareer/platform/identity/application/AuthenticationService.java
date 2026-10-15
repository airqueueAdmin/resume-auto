package com.aicareer.platform.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import com.aicareer.platform.identity.domain.RefreshSession;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.RefreshSessionRepository;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.identity.infrastructure.security.JwtProperties;
import com.aicareer.platform.identity.infrastructure.security.JwtTokenService;
import com.aicareer.platform.identity.infrastructure.security.TokenHashing;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthenticationService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public User register(String rawEmail, String rawPassword, String displayName) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.create(
                email,
                passwordEncoder.encode(rawPassword),
                displayName.trim(),
                clock.instant()
        );
        return userRepository.save(user);
    }

    @Transactional
    public AuthenticationResult login(String rawEmail, String rawPassword) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(rawEmail))
                .filter(User::canAuthenticate)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
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
