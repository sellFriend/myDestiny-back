package com.mydestiny.dto.block;

import jakarta.validation.constraints.NotBlank;

public record BlockRequest(
        @NotBlank String acquaintanceId
) {}
