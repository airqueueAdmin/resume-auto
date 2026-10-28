package com.aicareer.platform.shared.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return DataEncryption.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return DataEncryption.decrypt(dbData);
    }
}
