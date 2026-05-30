package com.mydestiny.dto.user;

import com.mydestiny.domain.User;

public record UserInfoResponse(
        String id,
        String email,
        String nickname,
        String role,
        boolean phoneVerified,
        String maskedPhone  // "010-****-5678" 또는 null
) {
    public static UserInfoResponse from(User user, String maskedPhone) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.isPhoneVerified(),
                maskedPhone
        );
    }
}
