package com.mydestiny.repository;

import com.mydestiny.domain.Notification;
import com.mydestiny.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    Optional<Notification> findByIdAndUserId(String id, String userId);

    List<Notification> findByUserIdAndMatchingIdAndTypeAndIsReadFalse(
            String userId, String matchingId, NotificationType type);
}
