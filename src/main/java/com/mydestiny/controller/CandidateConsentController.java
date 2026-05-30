package com.mydestiny.controller;

import com.mydestiny.dto.matching.ConsentApproveResponse;
import com.mydestiny.dto.matching.ConsentResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.CandidateConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidate-consents")
@RequiredArgsConstructor
public class CandidateConsentController {

    private final CandidateConsentService consentService;

    // 내 프로필에 대한 동의 대기 목록 (당사자 B 또는 D 기준)
    @GetMapping("/pending")
    public ApiResponse<List<ConsentResponse>> getPendingConsents(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(consentService.getPendingConsents(userId));
    }

    // 동의
    @PostMapping("/{id}/approve")
    public ApiResponse<ConsentApproveResponse> approveConsent(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return ApiResponse.ok(consentService.approveConsent(id, userId));
    }

    // 거절
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> rejectConsent(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        consentService.rejectConsent(id, userId);
        return ApiResponse.ok(null);
    }
}
