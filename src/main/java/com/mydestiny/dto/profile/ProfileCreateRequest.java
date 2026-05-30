package com.mydestiny.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(18) @Max(99) Integer age,
        String gender,          // "male" | "female" | "other" | null
        @NotNull Boolean isStudent,
        @Size(max = 100) String schoolName,
        @Size(max = 100) String major,
        @Size(max = 100) String occupation,
        @Size(max = 10) String mbti,
        @Size(max = 200) String hobby,
        @Size(max = 2000) String introduction,
        @NotBlank @Pattern(regexp = "^0[0-9]{8,10}$", message = "유효한 휴대폰 번호를 입력해주세요")
        String subjectPhone,
        @Size(max = 100) String kakaoId,
        @Size(max = 100) String instagramId
) {}
