package com.aicareer.platform.tool.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.identity.domain.User;
import org.junit.jupiter.api.Test;

class ToolApprovalTest {

    @Test
    void approvalCanBeConsumedOnlyOnceBeforeExpiry() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        ToolApproval approval = ToolApproval.create(
                user,
                "career.resume-draft.approve",
                "arguments-hash",
                "token-hash",
                issuedAt.plusSeconds(300),
                issuedAt
        );

        assertThat(approval.belongsTo(userId)).isTrue();
        assertThat(approval.matches(
                "career.resume-draft.approve",
                "arguments-hash",
                issuedAt.plusSeconds(1)
        )).isTrue();

        approval.consume(issuedAt.plusSeconds(2));

        assertThat(approval.matches(
                "career.resume-draft.approve",
                "arguments-hash",
                issuedAt.plusSeconds(3)
        )).isFalse();
        assertThatThrownBy(() -> approval.consume(issuedAt.plusSeconds(4)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiredApprovalDoesNotMatch() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        ToolApproval approval = ToolApproval.create(
                user,
                "career.resume-draft.approve",
                "arguments-hash",
                "token-hash",
                issuedAt.plusSeconds(300),
                issuedAt
        );

        assertThat(approval.matches(
                "career.resume-draft.approve",
                "arguments-hash",
                issuedAt.plusSeconds(300)
        )).isFalse();
    }
}
