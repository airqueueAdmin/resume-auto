package com.aicareer.platform.career.experience.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

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
                "Description",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(experience.getCompanyName()).isEqualTo("Company");
        assertThat(experience.getRole()).isEqualTo("Developer");
        assertThat(experience.getDescription()).isEqualTo("Description");
    }
}
