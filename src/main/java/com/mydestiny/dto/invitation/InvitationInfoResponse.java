package com.mydestiny.dto.invitation;

import com.mydestiny.domain.DatingProfile;

public record InvitationInfoResponse(
        String profileId,
        String registrantNickname,  // "OOO님이 소개팅 후보로 등록했습니다"
        String subjectName,
        String status
) {
    public static InvitationInfoResponse from(DatingProfile profile) {
        return new InvitationInfoResponse(
                profile.getId(),
                profile.getRegistrant().getNickname(),
                profile.getName(),
                profile.getStatus().name()
        );
    }
}
