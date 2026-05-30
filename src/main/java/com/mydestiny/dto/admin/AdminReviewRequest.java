package com.mydestiny.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReviewRequest(
        @NotBlank String decision,  // "APPROVE" | "REJECT"
        @Size(max = 500) String note
) {}
