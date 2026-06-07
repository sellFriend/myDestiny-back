package com.mydestiny.dto.acquaintance;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.ProfilePhoto;

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
        String visibility,
        LocalDateTime verifiedAt,
        List<String> photoUrls
) {
    public static AcquaintanceDetailResponse from(DatingProfile p) {
        List<String> photos = p.getPhotos() == null ? List.of() :
                p.getPhotos().stream()
                        .sorted(java.util.Comparator.comparingInt(ProfilePhoto::getDisplayOrder))
                        .map(ProfilePhoto::getImageUrl)
                        .toList();
        return new AcquaintanceDetailResponse(
                p.getId(), p.getName(), p.getAge(),
                p.getGender() != null ? p.getGender().getDbValue() : null,
                p.getOccupation(), p.getIntroduction(), p.getMbti(), p.getHobby(),
                p.getStatus().name(),
                p.getVisibility() != null ? p.getVisibility().name() : null,
                p.getPublishedAt(), photos
        );
    }
}
