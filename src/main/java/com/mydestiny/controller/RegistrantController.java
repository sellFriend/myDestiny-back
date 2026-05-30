package com.mydestiny.controller;

import com.mydestiny.dto.registrant.RegistrantBioUpdateRequest;
import com.mydestiny.dto.registrant.RegistrantSummaryResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.RegistrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrants")
@RequiredArgsConstructor
public class RegistrantController {

    private final RegistrantService registrantService;

    @GetMapping
    public ApiResponse<List<RegistrantSummaryResponse>> list(
            @AuthenticationPrincipal String currentUserId) {
        return ApiResponse.ok(registrantService.getRegistrants(currentUserId));
    }

    @GetMapping("/{userId}")
    public ApiResponse<RegistrantSummaryResponse> get(
            @PathVariable String userId,
            @AuthenticationPrincipal String currentUserId) {
        return ApiResponse.ok(registrantService.getRegistrant(userId, currentUserId));
    }

    @GetMapping("/{userId}/following")
    public ApiResponse<List<RegistrantSummaryResponse>> following(
            @PathVariable String userId,
            @AuthenticationPrincipal String currentUserId) {
        return ApiResponse.ok(registrantService.getFollowing(userId, currentUserId));
    }

    @GetMapping("/{userId}/followers")
    public ApiResponse<List<RegistrantSummaryResponse>> followers(
            @PathVariable String userId,
            @AuthenticationPrincipal String currentUserId) {
        return ApiResponse.ok(registrantService.getFollowers(userId, currentUserId));
    }

    @PatchMapping("/me/bio")
    public ApiResponse<Void> updateBio(
            @AuthenticationPrincipal String currentUserId,
            @Valid @RequestBody RegistrantBioUpdateRequest req) {
        registrantService.updateBio(currentUserId, req.bio());
        return ApiResponse.ok(null);
    }
}
