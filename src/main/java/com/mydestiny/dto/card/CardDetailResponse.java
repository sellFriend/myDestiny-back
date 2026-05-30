package com.mydestiny.dto.card;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.AcquaintancePhoto;

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
    public static CardDetailResponse from(Acquaintance a) {
        List<String> photos = a.getPhotos() == null ? List.of() :
                a.getPhotos().stream()
                        .sorted(Comparator.comparingInt(AcquaintancePhoto::getDisplayOrder))
                        .map(AcquaintancePhoto::getImageUrl)
                        .toList();
        return new CardDetailResponse(
                a.getId(), a.getName(), a.getAge(),
                a.getGender() != null ? a.getGender().getDbValue() : null,
                a.getJob(), a.getIntro(), a.getMbti(), a.getHobbies(), photos
        );
    }
}
