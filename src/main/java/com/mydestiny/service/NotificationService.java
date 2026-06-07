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
                // verification_done은 DB에는 기록하되 API 응답에서는 제외
                .filter(notification -> notification.getType() != NotificationType.VERIFICATION_DONE)
                .map(NotificationResponse::from)
                .toList();
    }

    // 종결된 액션의 미읽음 알림을 읽음 처리 — 종결된 카드에 알림으로 재진입하는 것을 차단
    @Transactional
    public void markResolved(String userId, NotificationType type, String targetId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndMatchingIdAndTypeAndIsReadFalse(userId, targetId, type);
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        notification.markAsRead();
        notificationRepository.save(notification);
    }
}
