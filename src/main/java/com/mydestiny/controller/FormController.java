package com.mydestiny.controller;

import com.mydestiny.dto.acquaintance.FormDataRequest;
import com.mydestiny.dto.acquaintance.FormDataResponse;
import com.mydestiny.dto.acquaintance.FormPrefillResponse;
import com.mydestiny.dto.acquaintance.PhotoResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.AcquaintanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class FormController {

    private final AcquaintanceService acquaintanceService;

    // 폼 진입 — 인증 옵셔널. 비로그인 허용(public), 로그인 상태면 본인 폼 차단.
    // 로그인된 친구의 기존 작성분이 있으면 draft에 prefill 데이터, 없으면 draft=null.
    @GetMapping("/{madamId}")
    public ResponseEntity<ApiResponse<FormPrefillResponse>> validateForm(
            @PathVariable String madamId,
            @AuthenticationPrincipal String userId) {
        FormPrefillResponse response = acquaintanceService.getFormState(madamId, userId);
        return ResponseEntity.ok(ApiResponse.ok("유효한 폼 링크입니다.", response));
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

    // 사진 목록 조회 — 교체(PUT) 전 photoId 확인용 (인증 필요)
    @GetMapping("/{uploadToken}/photos")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> getPhotos(@PathVariable String uploadToken) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.getPhotos(uploadToken)));
    }

    // 사진 업로드 — submitForm 응답의 uploadToken 사용 (인증 필요)
    @PostMapping("/{uploadToken}/photos")
    public ResponseEntity<ApiResponse<PhotoResponse>> uploadPhoto(
            @PathVariable String uploadToken,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.uploadPhoto(uploadToken, file)));
    }

    // 사진 교체 — 기존 파일을 삭제하고 새 파일로 대체 (인증 필요)
    @PutMapping("/{uploadToken}/photos/{photoId}")
    public ResponseEntity<ApiResponse<PhotoResponse>> replacePhoto(
            @PathVariable String uploadToken,
            @PathVariable String photoId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(acquaintanceService.replacePhoto(uploadToken, photoId, file)));
    }
}
