package com.mydestiny.dto.profile;

import com.mydestiny.domain.DatingProfile;

import java.time.LocalDateTime;
import java.util.List;

public record ProfileDetailResponse(
        String id,
        String registrantId,
        String registrantNickname,
        String status,
        String visibility,
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
        String kakaoId,
        String instagramId,
        String subjectPhone,   // 등록자(A)에게만 노출, 그 외 null
        List<String> photoUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProfileDetailResponse from(DatingProfile p, String decryptedPhone) {
        return build(p, p.getKakaoId(), p.getInstagramId(), decryptedPhone);
    }

    /** 비소유자 공개 조회용 — 연락처·전화번호 제외 */
    public static ProfileDetailResponse fromPublic(DatingProfile p) {
        return build(p, null, null, null);
    }

    private static ProfileDetailResponse build(DatingProfile p, String kakaoId, String instagramId, String subjectPhone) {
        List<String> photos = p.getPhotos().stream()
                .map(photo -> photo.getImageUrl())
                .toList();

        return new ProfileDetailResponse(
                p.getId(),
                p.getRegistrant().getId(),
                p.getRegistrant().getNickname(),
                p.getStatus().name(),
                p.getVisibility() != null ? p.getVisibility().name() : null,
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
                kakaoId,
                instagramId,
                subjectPhone,
                photos,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
