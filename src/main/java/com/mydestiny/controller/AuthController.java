package com.mydestiny.controller;

import com.mydestiny.dto.auth.TokenResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(accessToken, null)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
