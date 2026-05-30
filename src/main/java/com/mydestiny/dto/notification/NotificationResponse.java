package com.mydestiny.dto.notification;

import com.mydestiny.domain.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        String type,
        String matchingId,
        String consentId,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().getDbValue(),
                n.getMatchingId(),
                n.getConsentId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
