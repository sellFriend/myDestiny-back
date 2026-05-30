package com.mydestiny.dto.invitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^0[0-9]{8,10}$", message = "유효한 휴대폰 번호를 입력해주세요")
        String phone,

        @NotBlank @Size(min = 6, max = 6, message = "OTP는 6자리입니다")
        String otp,

        @NotBlank
        String invitationToken
) {}
