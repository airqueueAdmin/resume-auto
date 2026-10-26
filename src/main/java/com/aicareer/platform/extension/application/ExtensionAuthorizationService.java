package com.aicareer.platform.extension.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.extension.domain.ExtensionAuthorizationCode;
import com.aicareer.platform.extension.infrastructure.persistence.ExtensionAuthorizationCodeRepository;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.identity.infrastructure.security.JwtProperties;
import com.aicareer.platform.identity.infrastructure.security.JwtTokenService;
import com.aicareer.platform.identity.infrastructure.security.TokenHashing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtensionAuthorizationService {

    private final ExtensionAuthorizationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final ExtensionProperties extensionProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ExtensionAuthorizationService(
            ExtensionAuthorizationCodeRepository codeRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            ExtensionProperties extensionProperties,
            Clock clock
    ) {
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.extensionProperties = extensionProperties;
        this.clock = clock;
    }

    @Transactional
    public AuthorizationCode issue(UUID userId) {
        User user = activeUser(userId);
        Instant now = clock.instant();
        String rawCode = generateCode();
        Instant expiresAt = now.plus(extensionProperties.authorizationCodeTtl());
        codeRepository.save(ExtensionAuthorizationCode.create(
                user,
                TokenHashing.sha256(rawCode),
                expiresAt,
                now
        ));
        return new AuthorizationCode(rawCode, expiresAt);
    }

    @Transactional
    public ExtensionAuthenticationResult exchange(String rawCode) {
        Instant now = clock.instant();
        ExtensionAuthorizationCode code = codeRepository
                .findByCodeHash(TokenHashing.sha256(rawCode.trim()))
                .filter(item -> item.isActive(now))
                .orElseThrow(InvalidExtensionAuthorizationCodeException::new);
        code.consume(now);
        User user = code.getUser();
        if (!user.canAuthenticate()) {
            throw new InvalidExtensionAuthorizationCodeException();
        }
        return new ExtensionAuthenticationResult(
                user,
                jwtTokenService.createAccessToken(user),
                jwtProperties.accessTokenTtl()
        );
    }

    private User activeUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(UserNotFoundException::new);
    }

    private String generateCode() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record AuthorizationCode(String value, Instant expiresAt) {
    }
}
