package com.mydestiny.dto.matching;

import jakarta.validation.constraints.Size;

public record MatchingCancelRequest(
        @Size(max = 200) String reason
) {}
