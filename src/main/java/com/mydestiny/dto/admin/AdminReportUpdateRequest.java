package com.mydestiny.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminReportUpdateRequest(
        @NotBlank String status  // REVIEWING | RESOLVED | DISMISSED
) {}
