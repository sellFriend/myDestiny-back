package com.mydestiny.dto.card;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.AcquaintancePhoto;

import java.util.Comparator;
import java.util.List;

public record CardListResponse(
        String id,
        String name,
        Integer age,
        String gender,
        String mbti,
        String thumbnail
) {
    public static CardListResponse from(Acquaintance a) {
        String thumbnail = null;
        if (a.getPhotos() != null && !a.getPhotos().isEmpty()) {
            thumbnail = a.getPhotos().stream()
                    .min(Comparator.comparingInt(AcquaintancePhoto::getDisplayOrder))
                    .map(AcquaintancePhoto::getImageUrl)
                    .orElse(null);
        }
        return new CardListResponse(
                a.getId(), a.getName(), a.getAge(),
                a.getGender() != null ? a.getGender().getDbValue() : null,
                a.getMbti(), thumbnail
        );
    }
}
