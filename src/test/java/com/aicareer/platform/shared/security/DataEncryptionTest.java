package com.aicareer.platform.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataEncryptionTest {

    private static final String KEY = "REDACTED_HISTORY_VALUE";

    @BeforeEach
    void configureKey() {
        DataEncryption.configure(KEY);
    }

    @Test
    void encryptsWithRandomIvAndDecryptsTheOriginalValue() {
        String first = DataEncryption.encrypt("member@example.com");
        String second = DataEncryption.encrypt("member@example.com");

        assertThat(first).startsWith(DataEncryption.PREFIX).doesNotContain("member@example.com");
        assertThat(second).startsWith(DataEncryption.PREFIX).isNotEqualTo(first);
        assertThat(DataEncryption.decrypt(first)).isEqualTo("member@example.com");
        assertThat(DataEncryption.decrypt(second)).isEqualTo("member@example.com");
    }

    @Test
    void createsStableBlindIndexWithoutExposingTheInput() {
        String first = DataEncryption.blindIndex("member@example.com");
        String second = DataEncryption.blindIndex("member@example.com");

        assertThat(first).hasSize(64).isEqualTo(second).doesNotContain("member@example.com");
        assertThat(first).isNotEqualTo(DataEncryption.blindIndex("other@example.com"));
    }
}
