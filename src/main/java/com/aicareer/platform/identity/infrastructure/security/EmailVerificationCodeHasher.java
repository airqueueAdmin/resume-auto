package com.aicareer.platform.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.aicareer.platform.identity.application.EmailVerificationProperties;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationCodeHasher {

    private final byte[] secret;

    public EmailVerificationCodeHasher(EmailVerificationProperties properties) {
        this.secret = Base64.getDecoder().decode(properties.codeHmacSecretBase64());
    }

    public String hash(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.US_ASCII)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Could not hash email verification code", exception);
        }
    }

    public boolean matches(String code, String expectedHash) {
        return MessageDigest.isEqual(
                hash(code).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
