package com.mydestiny.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Gender {
    MALE("MALE"), FEMALE("FEMALE"), OTHER("OTHER");

    private final String dbValue;

    Gender(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    // DB·외부 표현은 대문자("FEMALE")로 통일. 과거 소문자 데이터/입력도 대소문자 무관하게 허용한다.
    public static Gender fromDb(String value) {
        for (Gender g : values()) {
            if (g.dbValue.equalsIgnoreCase(value) || g.name().equalsIgnoreCase(value)) return g;
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
