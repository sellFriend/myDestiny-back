package com.mydestiny.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum RegistrationStatus {
    DRAFT("draft"),
    VERIFICATION_PENDING("verification_pending"),
    VERIFIED("verified");

    private final String dbValue;

    RegistrationStatus(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static RegistrationStatus fromDb(String value) {
        for (RegistrationStatus s : values()) {
            if (s.dbValue.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown RegistrationStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class RegistrationStatusConverter implements AttributeConverter<RegistrationStatus, String> {
        @Override
        public String convertToDatabaseColumn(RegistrationStatus a) {
            return a == null ? null : a.getDbValue();
        }
        @Override
        public RegistrationStatus convertToEntityAttribute(String d) {
            return d == null ? null : RegistrationStatus.fromDb(d);
        }
    }
}
