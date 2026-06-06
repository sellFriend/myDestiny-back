package com.mydestiny.dto.card;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.ProfilePhoto;

import java.util.Comparator;
import java.util.List;

public record CardDetailResponse(
        String id,
        String name,
        Integer age,
        String gender,
        String job,
        String intro,
        String mbti,
        String hobbies,
        List<String> photoUrls
) {
    public static CardDetailResponse from(DatingProfile p) {
        List<String> photos = p.getPhotos() == null ? List.of() :
                p.getPhotos().stream()
                        .sorted(Comparator.comparingInt(ProfilePhoto::getDisplayOrder))
                        .map(ProfilePhoto::getImageUrl)
                        .toList();
        return new CardDetailResponse(
                p.getId(), p.getName(), p.getAge(),
                p.getGender() != null ? p.getGender().getDbValue() : null,
                p.getOccupation(), p.getIntroduction(), p.getMbti(), p.getHobby(), photos
        );
    }
}
