package com.mydestiny.dto.acquaintance;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.ProfilePhoto;

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
            boolean isStudent,
            String schoolName,
            String major,
            String job,
            String intro,
            String mbti,
            String hobbies,
            String phoneNumber,
            String kakaoId,
            String instagramId,
            List<String> photoUrls
    ) {
        public static Draft from(DatingProfile p, String phoneNumber) {
            List<String> photos = p.getPhotos() == null ? List.of() :
                    p.getPhotos().stream()
                            .sorted(Comparator.comparingInt(ProfilePhoto::getDisplayOrder))
                            .map(ProfilePhoto::getImageUrl)
                            .toList();
            return new Draft(
                    p.getId(),
                    p.getUploadToken(),
                    p.getStatus().name(),
                    p.getName(),
                    p.getAge(),
                    p.getGender() != null ? p.getGender().getDbValue() : null,
                    p.isStudent(),
                    p.getSchoolName(),
                    p.getMajor(),
                    p.getOccupation(),
                    p.getIntroduction(),
                    p.getMbti(),
                    p.getHobby(),
                    phoneNumber,
                    p.getKakaoId(),
                    p.getInstagramId(),
                    photos
            );
        }
    }

    public static FormPrefillResponse empty() {
        return new FormPrefillResponse(null);
    }

    public static FormPrefillResponse of(DatingProfile p, String phoneNumber) {
        return new FormPrefillResponse(Draft.from(p, phoneNumber));
    }
}
