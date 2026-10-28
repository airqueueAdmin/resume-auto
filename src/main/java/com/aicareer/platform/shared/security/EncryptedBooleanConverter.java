package com.aicareer.platform.shared.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedBooleanConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return attribute == null ? null : DataEncryption.encrypt(Boolean.toString(attribute));
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Boolean.valueOf(DataEncryption.decrypt(dbData));
    }
}
