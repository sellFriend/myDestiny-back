package com.mydestiny.service;

import com.mydestiny.domain.Notification;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.dto.notification.NotificationResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void create(String userId, NotificationType type, String matchingId) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .matchingId(matchingId)
                .build());
    }

    public void createWithConsent(String userId, NotificationType type, String matchingId, String consentId) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .matchingId(matchingId)
                .consentId(consentId)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(String userId) {
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        notification.markAsRead();
        notificationRepository.save(notification);
    }
}
