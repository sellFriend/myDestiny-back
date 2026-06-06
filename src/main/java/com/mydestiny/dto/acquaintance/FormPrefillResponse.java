package com.mydestiny.dto.acquaintance;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.AcquaintancePhoto;

import java.util.Comparator;
import java.util.List;

// 친구가 폼 진입 시 응답.
// draft가 null이면 처음 작성하는 상태, null이 아니면 기존 작성분이 있어 수정하는 상태.
public record FormPrefillResponse(Draft draft) {

    public record Draft(
            String acquaintanceId,
            String uploadToken,
            String status,
            String name,
            Integer age,
            String gender,
            String job,
            String intro,
            String mbti,
            String hobbies,
            String phoneNumber,
            String kakaoId,
            String instagramId,
            List<String> photoUrls
    ) {
        public static Draft from(Acquaintance a) {
            List<String> photos = a.getPhotos() == null ? List.of() :
                    a.getPhotos().stream()
                            .sorted(Comparator.comparingInt(AcquaintancePhoto::getDisplayOrder))
                            .map(AcquaintancePhoto::getImageUrl)
                            .toList();
            return new Draft(
                    a.getId(),
                    a.getVerificationToken(),
                    a.getRegistrationStatus().getDbValue(),
                    a.getName(),
                    a.getAge(),
                    a.getGender() != null ? a.getGender().getDbValue() : null,
                    a.getJob(),
                    a.getIntro(),
                    a.getMbti(),
                    a.getHobbies(),
                    a.getPhoneNumber(),
                    a.getKakaoId(),
                    a.getInstagramId(),
                    photos
            );
        }
    }

    public static FormPrefillResponse empty() {
        return new FormPrefillResponse(null);
    }

    public static FormPrefillResponse of(Acquaintance a) {
        return new FormPrefillResponse(Draft.from(a));
    }
}
