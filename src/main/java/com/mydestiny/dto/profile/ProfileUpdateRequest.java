package com.mydestiny.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 100) String name,
        @Min(18) @Max(99) Integer age,
        String gender,
        Boolean isStudent,
        @Size(max = 100) String schoolName,
        @Size(max = 100) String major,
        @Size(max = 100) String occupation,
        @Size(max = 10) String mbti,
        @Size(max = 200) String hobby,
        @Size(max = 2000) String introduction,
        @Size(max = 100) String kakaoId,
        @Size(max = 100) String instagramId
) {}
