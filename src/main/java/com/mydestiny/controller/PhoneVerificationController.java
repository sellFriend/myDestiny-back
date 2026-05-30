package com.mydestiny.controller;

import com.mydestiny.dto.invitation.OtpVerifyRequest;
import com.mydestiny.dto.invitation.OtpVerifyResponse;
import com.mydestiny.dto.invitation.PhoneVerificationSendRequest;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/phone-verifications")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PhoneVerificationSendRequest request) {
        phoneVerificationService.sendOtp(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("OTP가 발송됐습니다.", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verify(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(phoneVerificationService.verifyOtp(userId, request)));
    }
}
