package com.mydestiny.controller;

import com.mydestiny.dto.acquaintance.AcquaintanceDetailResponse;
import com.mydestiny.dto.acquaintance.InviteResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.AcquaintanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/acquaintances")
@RequiredArgsConstructor
public class AcquaintanceController {

    private final AcquaintanceService acquaintanceService;

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<InviteResponse>> invite(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.createInvite(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AcquaintanceDetailResponse>> getDetail(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.getAcquaintance(id, userId)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        acquaintanceService.approve(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        acquaintanceService.reject(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
