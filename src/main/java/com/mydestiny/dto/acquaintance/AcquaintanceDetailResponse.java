package com.mydestiny.dto.acquaintance;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.AcquaintancePhoto;

import java.time.LocalDateTime;
import java.util.List;

public record AcquaintanceDetailResponse(
        String id,
        String name,
        Integer age,
        String gender,
        String job,
        String intro,
        String mbti,
        String hobbies,
        String registrationStatus,
        LocalDateTime verifiedAt,
        List<String> photoUrls
) {
    public static AcquaintanceDetailResponse from(Acquaintance a) {
        List<String> photos = a.getPhotos() == null ? List.of() :
                a.getPhotos().stream()
                        .sorted(java.util.Comparator.comparingInt(AcquaintancePhoto::getDisplayOrder))
                        .map(AcquaintancePhoto::getImageUrl)
                        .toList();
        return new AcquaintanceDetailResponse(
                a.getId(), a.getName(), a.getAge(),
                a.getGender() != null ? a.getGender().getDbValue() : null,
                a.getJob(), a.getIntro(), a.getMbti(), a.getHobbies(),
                a.getRegistrationStatus().getDbValue(),
                a.getVerifiedAt(), photos
        );
    }
}
