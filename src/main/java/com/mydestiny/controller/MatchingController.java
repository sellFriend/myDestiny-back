package com.mydestiny.controller;

import com.mydestiny.dto.matching.*;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matchings")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping
    public ApiResponse<MatchingResponse> createMatching(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody MatchingRequest request) {
        return ApiResponse.ok(matchingService.createMatching(userId, request));
    }

    @GetMapping("/sent")
    public ApiResponse<List<MatchingResponse>> getSentMatchings(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(matchingService.getSentMatchings(userId));
    }

    @GetMapping("/matched")
    public ApiResponse<List<MatchingResponse>> getMatchedMatchings(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(matchingService.getMatchedMatchings(userId));
    }

    @GetMapping("/received")
    public ApiResponse<List<MatchingResponse>> getReceivedMatchings(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(matchingService.getReceivedMatchings(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MatchingResponse> getMatchingDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return ApiResponse.ok(matchingService.getMatchingDetail(id, userId));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<MatchingResponse> acceptMatching(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return ApiResponse.ok(matchingService.acceptMatching(id, userId));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<MatchingResponse> rejectMatching(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @RequestBody(required = false) MatchingRejectRequest request) {
        String reason = request != null ? request.reason() : null;
        return ApiResponse.ok(matchingService.rejectMatching(id, userId, reason));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelMatching(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        matchingService.cancelMatching(id, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/contact")
    public ApiResponse<ContactResponse> getContact(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return ApiResponse.ok(matchingService.getContact(id, userId));
    }
}
