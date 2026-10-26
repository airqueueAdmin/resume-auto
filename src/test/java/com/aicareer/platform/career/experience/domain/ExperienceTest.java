package com.aicareer.platform.career.experience.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import com.aicareer.platform.identity.domain.User;
import org.junit.jupiter.api.Test;

class ExperienceTest {

    @Test
    void createsExperienceWithoutDates() {
        User user = User.create(
                "test@example.com",
                "{bcrypt}test",
                "Test User",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Experience experience = Experience.create(
                user,
                "Company",
                "Developer",
                null,
                null,
                false,
                "Description",
                "Java, Spring Boot",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(experience.getCompanyName()).isEqualTo("Company");
        assertThat(experience.getRole()).isEqualTo("Developer");
        assertThat(experience.getStartDate()).isNull();
        assertThat(experience.getEndDate()).isNull();
        assertThat(experience.isCurrentlyEmployed()).isFalse();
        assertThat(experience.getDescription()).isEqualTo("Description");
        assertThat(experience.getTechStack()).isEqualTo("Java, Spring Boot");
    }

    @Test
    void keepsOptionalPeriodAndCurrentEmploymentStatus() {
        User user = User.create(
                "current@example.com",
                "{bcrypt}test",
                "Current User",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Experience experience = Experience.create(
                user,
                "Company",
                "Developer",
                LocalDate.of(2024, 1, 1),
                null,
                true,
                "Description",
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(experience.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(experience.getEndDate()).isNull();
        assertThat(experience.isCurrentlyEmployed()).isTrue();
    }

    @Test
    void rejectsEndDateForCurrentEmployment() {
        User user = User.create(
                "invalid@example.com",
                "{bcrypt}test",
                "Invalid User",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThatThrownBy(() -> Experience.create(
                user,
                "Company",
                "Developer",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 1, 1),
                true,
                "Description",
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endDate must be empty when currentlyEmployed is true");
    }
}
