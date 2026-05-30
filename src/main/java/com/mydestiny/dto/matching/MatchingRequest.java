package com.mydestiny.dto.matching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatchingRequest(
        @NotBlank String requesterProfileId,   // 내가 등록한 친구 B의 프로필 ID
        @NotBlank String targetProfileId,       // 상대 친구 D의 프로필 ID
        @Size(max = 200) String message
) {}
