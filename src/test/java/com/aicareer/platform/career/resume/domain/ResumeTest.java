package com.aicareer.platform.career.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.aicareer.platform.identity.domain.User;
import org.junit.jupiter.api.Test;

class ResumeTest {

    @Test
    void versionNumbersAdvanceSequentially() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        User user = User.create("test@example.com", "{bcrypt}test", "Test User", now);
        Resume resume = Resume.create(user, "Resume", now);

        assertThat(resume.nextVersionNumber()).isEqualTo(1);

        resume.markVersionCreated(1, now);

        assertThat(resume.getLatestVersionNumber()).isEqualTo(1);
        assertThat(resume.nextVersionNumber()).isEqualTo(2);
    }

    @Test
    void rejectsNonSequentialVersionNumbers() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        User user = User.create("test@example.com", "{bcrypt}test", "Test User", now);
        Resume resume = Resume.create(user, "Resume", now);

        assertThatThrownBy(() -> resume.markVersionCreated(2, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("versionNumber must be the next sequential version");
    }
}
