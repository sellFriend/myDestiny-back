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
        boolean matched,              // 매칭 성사 여부 (MatchingStatus.MATCHED)
        boolean hasOutgoingRequest,   // 이 친구가 보낸 매칭 요청이 진행 중인지 (PENDING)
        LocalDateTime verifiedAt,
        List<String> photoUrls
) {
    public static AcquaintanceDetailResponse from(DatingProfile p, boolean matched, boolean hasOutgoingRequest) {
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
                matched,
                hasOutgoingRequest,
                p.getPublishedAt(), photos
        );
    }
}
