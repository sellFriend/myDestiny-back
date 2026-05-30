package com.mydestiny.controller;

import com.mydestiny.dto.acquaintance.FormDataRequest;
import com.mydestiny.dto.acquaintance.FormDataResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.AcquaintanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class FormController {

    private final AcquaintanceService acquaintanceService;

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<Void>> validateForm(@PathVariable String token) {
        acquaintanceService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.ok("유효한 초대 링크입니다.", null));
    }

    @PostMapping("/{token}")
    public ResponseEntity<ApiResponse<FormDataResponse>> submitForm(
            @PathVariable String token,
            @Valid @RequestBody FormDataRequest request) {
        FormDataResponse response = acquaintanceService.submitForm(token, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{token}/photos")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(
            @PathVariable String token,
            @RequestParam("file") MultipartFile file) {
        String url = acquaintanceService.uploadPhoto(token, file);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }
}
