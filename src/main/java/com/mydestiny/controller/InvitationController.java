package com.mydestiny.controller;

import com.mydestiny.dto.invitation.InvitationInfoResponse;
import com.mydestiny.dto.invitation.InviteCreateResponse;
import com.mydestiny.dto.invitation.RejectRequest;
import com.mydestiny.dto.profile.ProfileDetailResponse;
import com.mydestiny.dto.profile.ProfileUpdateRequest;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.ApprovalService;
import com.mydestiny.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final ApprovalService approvalService;

    // A가 초대 링크 생성
    @PostMapping("/api/profiles/{id}/invite")
    public ResponseEntity<ApiResponse<InviteCreateResponse>> createInvite(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.createInvite(id, userId)));
    }

    // B가 초대 링크 열기 (로그인 불필요 — public)
    @GetMapping("/api/invitations/{token}")
    public ResponseEntity<ApiResponse<InvitationInfoResponse>> getInfo(
            @PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.getInfo(token)));
    }

    // B가 자신의 프로필 조회 (로그인 + 번호 인증 후)
    @GetMapping("/api/invitations/{token}/profile")
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> getProfile(
            @PathVariable String token,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getProfileForSubject(token, userId)));
    }

    // B가 프로필 수정
    @PatchMapping("/api/invitations/{token}/profile")
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> updateProfile(
            @PathVariable String token,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.updateProfileAsSubject(token, userId, request)));
    }

    // B가 개인정보 동의
    @PostMapping("/api/invitations/{token}/consent")
    public ResponseEntity<ApiResponse<Void>> consent(
            @PathVariable String token,
            @AuthenticationPrincipal String userId) {
        approvalService.recordConsent(token, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // B가 승인
    @PostMapping("/api/invitations/{token}/approve")
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> approve(
            @PathVariable String token,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.approve(token, userId)));
    }

    // B가 거절
    @PostMapping("/api/invitations/{token}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable String token,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody(required = false) RejectRequest request) {
        approvalService.reject(token, userId, request != null ? request.reason() : null);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
