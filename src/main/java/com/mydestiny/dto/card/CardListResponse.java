package com.mydestiny.dto.card;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.ProfilePhoto;

import java.util.Comparator;

public record CardListResponse(
        String id,
        String name,
        Integer age,
        String gender,
        String mbti,
        String thumbnail
) {
    public static CardListResponse from(DatingProfile p) {
        String thumbnail = null;
        if (p.getPhotos() != null && !p.getPhotos().isEmpty()) {
            thumbnail = p.getPhotos().stream()
                    .min(Comparator.comparingInt(ProfilePhoto::getDisplayOrder))
                    .map(ProfilePhoto::getImageUrl)
                    .orElse(null);
        }
        return new CardListResponse(
                p.getId(), p.getName(), p.getAge(),
                p.getGender() != null ? p.getGender().getDbValue() : null,
                p.getMbti(), thumbnail
        );
    }
}
