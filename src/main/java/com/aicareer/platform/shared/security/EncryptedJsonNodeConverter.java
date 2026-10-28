package com.aicareer.platform.shared.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedJsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute == null ? null : DataEncryption.encrypt(attribute.toString());
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readTree(DataEncryption.decrypt(dbData));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read protected JSON data", exception);
        }
    }
}
