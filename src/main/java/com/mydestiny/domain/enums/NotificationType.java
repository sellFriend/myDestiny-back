package com.mydestiny.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum NotificationType {
    // 기존 타입
    FORM_SUBMITTED("form_submitted"),                           // 지인이 폼 제출 완료
    MATCH_REQUEST("match_request"),
    MATCH_ACCEPTED("match_accepted"),
    MATCH_REJECTED("match_rejected"),
    VERIFICATION_DONE("verification_done"),
    ACQUAINTANCE_BLOCKED("acquaintance_blocked"),

    // 새 매칭 플로우 타입
    MATCH_CONSENT_REQUESTED("match_consent_requested"),         // B/D에게 최종 동의 요청
    MATCH_COUNTERPART_CONSENTED("match_counterpart_consented"), // 상대방이 동의 (나머지 후보에게)
    MATCH_CONSENT_REJECTED("match_consent_rejected"),           // 동의 거절로 매칭 실패
    MATCHED("matched"),                                         // 매칭 성사
    MATCH_REQUEST_EXPIRED("match_request_expired"),             // 수신자 응답 기한 만료
    MATCH_CONSENT_EXPIRED("match_consent_expired"),             // 동의 기한 만료
    EDIT_REQUESTED("edit_requested");                           // 주선자가 친구에게 폼 수정 요청

    private final String dbValue;

    NotificationType(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static NotificationType fromDb(String value) {
        for (NotificationType t : values()) {
            if (t.dbValue.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown NotificationType: " + value);
    }

    @Converter(autoApply = true)
    public static class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {
        @Override
        public String convertToDatabaseColumn(NotificationType a) {
            return a == null ? null : a.getDbValue();
        }
        @Override
        public NotificationType convertToEntityAttribute(String d) {
            return d == null ? null : NotificationType.fromDb(d);
        }
    }
}
