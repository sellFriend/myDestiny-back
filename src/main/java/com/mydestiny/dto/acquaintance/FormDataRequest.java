package com.mydestiny.dto.acquaintance;

import jakarta.validation.constraints.*;

public record FormDataRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(10) @Max(99) Integer age,
        String gender,
        @Size(max = 100) String job,
        String intro,
        @Pattern(regexp = "^[A-Z]{4}$", message = "MBTI는 대문자 4자리여야 합니다") String mbti,
        String hobbies,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 100) String kakaoId,
        @Size(max = 100) String instagramId
) {}
