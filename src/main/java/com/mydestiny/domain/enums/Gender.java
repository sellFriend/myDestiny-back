package com.mydestiny.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Gender {
    MALE("male"), FEMALE("female"), OTHER("other");

    private final String dbValue;

    Gender(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static Gender fromDb(String value) {
        for (Gender g : values()) {
            if (g.dbValue.equals(value)) return g;
        }
        throw new IllegalArgumentException("Unknown Gender: " + value);
    }

    @Converter(autoApply = true)
    public static class GenderConverter implements AttributeConverter<Gender, String> {
        @Override
        public String convertToDatabaseColumn(Gender a) {
            return a == null ? null : a.getDbValue();
        }
        @Override
        public Gender convertToEntityAttribute(String d) {
            return d == null ? null : Gender.fromDb(d);
        }
    }
}
