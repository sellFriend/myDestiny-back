package com.mydestiny.dto.acquaintance;

import jakarta.validation.constraints.*;

public record FormDataRequest(
        boolean useKakaoPhoto,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(10) @Max(99) Integer age,
        String gender,
        @NotNull Boolean isStudent,
        @Size(max = 100) String schoolName,
        @Size(max = 100) String major,
        @Size(max = 100) String job,
        String intro,
        @Pattern(regexp = "^[A-Z]{4}$", message = "MBTI는 대문자 4자리여야 합니다") String mbti,
        String hobbies,
        @NotBlank @Size(max = 20) String phoneNumber,
        @Size(max = 100) String kakaoId,
        @Size(max = 100) String instagramId
) {
    // 학생이면 학교/전공 필수, 비학생이면 직업 필수
    @AssertTrue(message = "학생은 학교·전공을, 비학생은 직업을 입력해야 합니다")
    public boolean isOccupationInfoValid() {
        if (Boolean.TRUE.equals(isStudent)) {
            return schoolName != null && !schoolName.isBlank()
                    && major != null && !major.isBlank();
        }
        return job != null && !job.isBlank();
    }
}
