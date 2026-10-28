package com.aicareer.platform.shared.security;

import org.springframework.stereotype.Service;

@Service
public class DataEncryptionService {

    public DataEncryptionService(DataEncryptionProperties properties) {
        DataEncryption.configure(properties.keyBase64());
    }

    public String encrypt(String value) {
        return DataEncryption.encrypt(value);
    }

    public String decrypt(String value) {
        return DataEncryption.decrypt(value);
    }

    public String blindIndex(String normalizedValue) {
        return DataEncryption.blindIndex(normalizedValue);
    }
}
