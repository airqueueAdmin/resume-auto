package com.aicareer.platform.tool.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.identity.infrastructure.persistence.UserRepository;
import com.aicareer.platform.tool.domain.ToolApproval;
import com.aicareer.platform.tool.infrastructure.config.ToolProperties;
import com.aicareer.platform.tool.infrastructure.persistence.ToolApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolApprovalService {

    private final ToolRegistry toolRegistry;
    private final ToolApprovalRepository toolApprovalRepository;
    private final UserRepository userRepository;
    private final ToolProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ToolApprovalService(
            ToolRegistry toolRegistry,
            ToolApprovalRepository toolApprovalRepository,
            UserRepository userRepository,
            ToolProperties properties,
            Clock clock
    ) {
        this.toolRegistry = toolRegistry;
        this.toolApprovalRepository = toolApprovalRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedToolApproval issue(UUID userId, ToolApprovalCommand command) {
        ValidatedToolCall call = toolRegistry.validate(command.toolName(), command.arguments());
        if (!call.definition().mutating()) {
            throw new ToolArgumentValidationException(java.util.Map.of(
                    "toolName", "explicit approval is only available for mutating tools"
            ));
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::canAuthenticate)
                .orElseThrow(com.aicareer.platform.identity.application.UserNotFoundException::new);

        Instant now = clock.instant();
        String token = randomToken();
        ToolApproval approval = ToolApproval.create(
                user,
                call.definition().name(),
                toolRegistry.argumentsHash(call.arguments()),
                hashToken(token),
                now.plus(properties.approvalTtl()),
                now
        );
        toolApprovalRepository.save(approval);
        return new IssuedToolApproval(token, now.plus(properties.approvalTtl()));
    }

    @Transactional
    public void consume(
            UUID userId,
            String token,
            ValidatedToolCall call
    ) {
        if (token == null || token.isBlank()) {
            throw new ToolApprovalRequiredException();
        }
        ToolApproval approval = toolApprovalRepository.findForUpdate(hashToken(token))
                .orElseThrow(ToolApprovalInvalidException::new);
        if (!approval.belongsTo(userId)
                || !approval.matches(call.definition().name(), toolRegistry.argumentsHash(call.arguments()), clock.instant())) {
            throw new ToolApprovalInvalidException();
        }
        approval.consume(clock.instant());
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    public record IssuedToolApproval(String token, Instant expiresAt) {
    }
}
