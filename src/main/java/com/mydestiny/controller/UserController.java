package com.mydestiny.controller;

import com.mydestiny.domain.User;
import com.mydestiny.dto.user.NicknameUpdateRequest;
import com.mydestiny.dto.user.UserInfoResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.repository.UserRepository;
import com.mydestiny.util.PhoneEncryptionUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PhoneEncryptionUtil phoneEncryptionUtil;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMe(
            @AuthenticationPrincipal String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        String maskedPhone = null;
        if (user.isPhoneVerified() && user.getPhoneNumberEncrypted() != null) {
            String plain = phoneEncryptionUtil.decrypt(user.getPhoneNumberEncrypted());
            maskedPhone = phoneEncryptionUtil.mask(plain);
        }

        return ResponseEntity.ok(ApiResponse.ok(UserInfoResponse.from(user, maskedPhone)));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<Void> updateNickname(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody NicknameUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        user.updateNickname(req.nickname());
        userRepository.save(user);
        return ApiResponse.ok(null);
    }
}
