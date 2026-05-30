package com.mydestiny.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Visibility {
    PUBLIC("public"), PRIVATE("private");

    private final String dbValue;

    Visibility(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static Visibility fromDb(String value) {
        for (Visibility v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown Visibility: " + value);
    }

    @Converter(autoApply = true)
    public static class VisibilityConverter implements AttributeConverter<Visibility, String> {
        @Override
        public String convertToDatabaseColumn(Visibility a) {
            return a == null ? null : a.getDbValue();
        }
        @Override
        public Visibility convertToEntityAttribute(String d) {
            return d == null ? null : Visibility.fromDb(d);
        }
    }
}
