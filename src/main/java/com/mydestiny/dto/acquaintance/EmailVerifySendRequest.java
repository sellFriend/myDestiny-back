package com.mydestiny.dto.acquaintance;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerifySendRequest(
        @NotBlank @Email @jakarta.validation.constraints.Size(max = 255) String email
) {}
