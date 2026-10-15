package com.aicareer.platform.career.project.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import com.aicareer.platform.identity.domain.User;
import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void rejectsEndDateBeforeStartDate() {
        User user = User.create(
                "test@example.com",
                "{bcrypt}test",
                "Test User",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThatThrownBy(() -> Project.create(
                user,
                "Project",
                "Developer",
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 1, 1),
                "Description",
                "Java",
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endDate must be on or after startDate");
    }
}
