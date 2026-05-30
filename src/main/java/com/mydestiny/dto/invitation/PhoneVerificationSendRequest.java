package com.mydestiny.dto.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationSendRequest(
        @NotBlank
        @Pattern(regexp = "^0[0-9]{8,10}$", message = "유효한 휴대폰 번호를 입력해주세요")
        String phone,

        @NotBlank
        String invitationToken,

        // SMS_PROVIDER=email 일 때 OTP를 받을 이메일 주소 (선택)
        @Email String email
) {}
