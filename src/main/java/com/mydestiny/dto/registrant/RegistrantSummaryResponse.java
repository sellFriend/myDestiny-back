package com.mydestiny.dto.registrant;

import com.mydestiny.domain.User;

public record RegistrantSummaryResponse(
        String id,
        String nickname,
        String bio,
        long publishedProfileCount,
        long followerCount,
        boolean isFollowing
) {
    public static RegistrantSummaryResponse of(User user, long publishedProfileCount,
                                               long followerCount, boolean isFollowing) {
        return new RegistrantSummaryResponse(
                user.getId(),
                user.getNickname(),
                user.getBio(),
                publishedProfileCount,
                followerCount,
                isFollowing
        );
    }
}
