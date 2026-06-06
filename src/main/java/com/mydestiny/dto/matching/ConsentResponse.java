package com.mydestiny.dto.matching;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.MatchCandidateConsent;
import com.mydestiny.domain.Matching;

import java.time.LocalDateTime;

public record ConsentResponse(
        String id,
        String matchingId,
        ProfileSummary myProfile,
        ProfileSummary counterpartProfile,
        String requesterNickname,   // A의 닉네임
        String receiverNickname,    // C의 닉네임
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public record ProfileSummary(String id, String name, String gender) {}

    public static ConsentResponse from(MatchCandidateConsent consent) {
        Matching m = consent.getMatching();
        DatingProfile myProfile = consent.getProfile();
        boolean isRequesterSide = myProfile.getId().equals(m.getRequesterProfile().getId());
        DatingProfile counterpart = isRequesterSide ? m.getTargetProfile() : m.getRequesterProfile();

        return new ConsentResponse(
                consent.getId(),
                m.getId(),
                toSummary(myProfile),
                toSummary(counterpart),
                m.getRequester().getNickname(),
                m.getReceiver().getNickname(),
                consent.getStatus().name(),
                consent.getExpiresAt(),
                consent.getCreatedAt()
        );
    }

    private static ProfileSummary toSummary(DatingProfile p) {
        return new ProfileSummary(
                p.getId(),
                p.getName(),
                p.getGender() != null ? p.getGender().getDbValue() : null
        );
    }
}
