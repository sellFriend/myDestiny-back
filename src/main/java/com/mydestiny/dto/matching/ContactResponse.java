package com.mydestiny.dto.matching;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.User;

public record ContactResponse(
        String name,
        String kakaoId,
        String instagramId
) {
    public static ContactResponse from(DatingProfile profile) {
        return new ContactResponse(
                profile.getName(),
                profile.getKakaoId(),
                profile.getInstagramId()
        );
    }
}
