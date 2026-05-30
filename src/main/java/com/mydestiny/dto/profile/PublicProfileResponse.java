package com.mydestiny.dto.profile;

import com.mydestiny.domain.DatingProfile;

public record PublicProfileResponse(
        String id,
        String name,
        Integer age,
        String gender,
        boolean isStudent,
        String schoolName,
        String major,
        String occupation,
        String mbti,
        String hobby,
        String introduction,
        String firstPhotoUrl
) {
    public static PublicProfileResponse from(DatingProfile p) {
        String photo = p.getPhotos().isEmpty() ? null : p.getPhotos().get(0).getImageUrl();
        return new PublicProfileResponse(
                p.getId(),
                p.getName(),
                p.getAge(),
                p.getGender() != null ? p.getGender().getDbValue() : null,
                p.isStudent(),
                p.getSchoolName(),
                p.getMajor(),
                p.getOccupation(),
                p.getMbti(),
                p.getHobby(),
                p.getIntroduction(),
                photo
        );
    }
}
