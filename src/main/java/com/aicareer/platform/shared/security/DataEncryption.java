package com.aicareer.platform.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class DataEncryption {

    public static final String PREFIX = "enc:v1:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile SecretKeySpec encryptionKey;

    private DataEncryption() {
    }

    public static void configure(String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank() || isUnresolvedPlaceholder(keyBase64)) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY_BASE64 must be configured with a random 32-byte Base64 key"
            );
        }
        byte[] key = decodeKey(keyBase64);
        encryptionKey = new SecretKeySpec(key, AES_ALGORITHM);
    }

    public static String encrypt(String value) {
        if (value == null) {
            return null;
        }
        if (isEncrypted(value)) {
            return value;
        }
        return encrypt(value, requireKey());
    }

    public static String decrypt(String value) {
        if (value == null || !isEncrypted(value)) {
            return value;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(value.substring(PREFIX.length()));
            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not decrypt protected data", exception);
        }
    }

    public static String blindIndex(String normalizedValue) {
        if (normalizedValue == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(requireKey().getEncoded(), HMAC_ALGORITHM));
            return HexFormatHolder.format(mac.doFinal(
                    ("blind-index:v1:" + normalizedValue).getBytes(StandardCharsets.UTF_8)
            ));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not create protected lookup index", exception);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    static String encrypt(String value, SecretKeySpec key) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt protected data", exception);
        }
    }

    static SecretKeySpec keyFromBase64(String keyBase64) {
        return new SecretKeySpec(decodeKey(keyBase64), AES_ALGORITHM);
    }

    private static SecretKeySpec requireKey() {
        SecretKeySpec key = encryptionKey;
        if (key == null) {
            String configured = System.getProperty("DATA_ENCRYPTION_KEY_BASE64");
            if (configured == null || configured.isBlank()) {
                configured = System.getenv("DATA_ENCRYPTION_KEY_BASE64");
            }
            if (configured != null && !configured.isBlank()) {
                configure(configured);
                key = encryptionKey;
            }
        }
        if (key == null) {
            throw new IllegalStateException("Data encryption key has not been initialized");
        }
        return key;
    }

    private static byte[] decodeKey(String keyBase64) {
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY_BASE64 is not valid Base64", exception);
        }
        if (decoded.length != KEY_BYTES) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY_BASE64 must decode to exactly 32 bytes");
        }
        return decoded;
    }

    private static boolean isUnresolvedPlaceholder(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("${") && trimmed.endsWith("}");
    }

    private static final class HexFormatHolder {
        private static String format(byte[] value) {
            return java.util.HexFormat.of().formatHex(value);
        }
    }
}
