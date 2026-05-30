package com.mydestiny.controller;

import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public ApiResponse<Void> follow(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String userId) {
        followService.follow(currentUserId, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{userId}/follow")
    public ApiResponse<Void> unfollow(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String userId) {
        followService.unfollow(currentUserId, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{userId}/follow-status")
    public ApiResponse<FollowService.FollowStatus> followStatus(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String userId) {
        return ApiResponse.ok(followService.getFollowStatus(currentUserId, userId));
    }
}
