package com.mydestiny.dto.acquaintance;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerifyConfirmRequest(
        @NotBlank @Email String email,
        @NotBlank String otp
) {}
