package com.mydestiny.controller;

import com.mydestiny.dto.acquaintance.AcquaintanceDetailResponse;
import com.mydestiny.dto.acquaintance.InviteResponse;
import java.util.List;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<AcquaintanceDetailResponse>>> getMyList(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.getMyAcquaintances(userId)));
    }

    // 마담 자신의 영구 폼 링크 조회 (없으면 최초 1회 생성)
    @GetMapping("/my-form")
    public ResponseEntity<ApiResponse<InviteResponse>> getMyFormLink(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "Origin", required = false) String origin) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.getFormLink(userId, origin)));
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

    // 주선자가 수정 요청 — 카드 상태를 DRAFT로 되돌리고 친구에게 알림 발송
    @PostMapping("/{id}/request-edit")
    public ResponseEntity<ApiResponse<Void>> requestEdit(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        acquaintanceService.requestEdit(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
