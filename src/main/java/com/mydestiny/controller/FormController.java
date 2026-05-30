package com.mydestiny.controller;

import com.mydestiny.dto.acquaintance.FormDataRequest;
import com.mydestiny.dto.acquaintance.FormDataResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.AcquaintanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class FormController {

    private final AcquaintanceService acquaintanceService;

    // 폼 링크 유효성 확인 — 인증 불필요 (public)
    @GetMapping("/{madamId}")
    public ResponseEntity<ApiResponse<Void>> validateForm(@PathVariable String madamId) {
        acquaintanceService.validateToken(madamId);
        return ResponseEntity.ok(ApiResponse.ok("유효한 폼 링크입니다.", null));
    }

    // 프로필 제출 — 카카오 로그인 후 호출 (인증 필요)
    @PostMapping("/{madamId}")
    public ResponseEntity<ApiResponse<FormDataResponse>> submitForm(
            @PathVariable String madamId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody FormDataRequest request) {
        FormDataResponse response = acquaintanceService.submitForm(madamId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 사진 업로드 — submitForm 응답의 uploadToken 사용 (인증 필요)
    @PostMapping("/{uploadToken}/photos")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(
            @PathVariable String uploadToken,
            @RequestParam("file") MultipartFile file) {
        String url = acquaintanceService.uploadPhoto(uploadToken, file);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }
}
