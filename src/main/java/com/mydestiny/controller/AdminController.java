package com.mydestiny.controller;

import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.admin.AdminMatchingResponse;
import com.mydestiny.dto.admin.AdminProfileResponse;
import com.mydestiny.dto.admin.AdminReportResponse;
import com.mydestiny.dto.admin.AdminReportUpdateRequest;
import com.mydestiny.dto.admin.AdminReviewRequest;
import com.mydestiny.dto.profile.ProfileDetailResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 상태별 프로필 목록 (예: ?status=REVIEW_REQUIRED)
    @GetMapping("/profiles")
    public ResponseEntity<ApiResponse<List<AdminProfileResponse>>> getProfiles(
            @AuthenticationPrincipal String adminId,
            @RequestParam(required = false) String status) {
        ProfileStatus profileStatus = status != null ? ProfileStatus.valueOf(status) : null;
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllByStatus(adminId, profileStatus)));
    }

    // 프로필 숨김
    @PatchMapping("/profiles/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(
            @AuthenticationPrincipal String adminId,
            @PathVariable String id) {
        adminService.suspend(adminId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 신고 목록 (예: ?status=PENDING)
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<AdminReportResponse>>> getReports(
            @AuthenticationPrincipal String adminId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getReports(adminId, status)));
    }

    // 신고 상태 업데이트
    @PatchMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<AdminReportResponse>> updateReport(
            @AuthenticationPrincipal String adminId,
            @PathVariable String id,
            @Valid @RequestBody AdminReportUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateReport(adminId, id, request)));
    }

    // 매칭 전체 현황
    @GetMapping("/matchings")
    public ResponseEntity<ApiResponse<List<AdminMatchingResponse>>> getMatchings(
            @AuthenticationPrincipal String adminId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getMatchings(adminId, status)));
    }

    // 관리자 승인/거절
    @PatchMapping("/profiles/{id}/review")
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> review(
            @AuthenticationPrincipal String adminId,
            @PathVariable String id,
            @Valid @RequestBody AdminReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.review(adminId, id, request)));
    }
}
