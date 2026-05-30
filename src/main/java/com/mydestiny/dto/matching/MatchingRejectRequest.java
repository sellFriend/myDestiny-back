package com.mydestiny.dto.matching;

import jakarta.validation.constraints.Size;

public record MatchingRejectRequest(
        @Size(max = 200) String reason
) {}
