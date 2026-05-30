package com.mydestiny.controller;

import com.mydestiny.dto.notification.NotificationResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(notificationService.getUnread(userId));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.ok(null);
    }
}
