package com.mydestiny.dto.matching;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.Matching;
import com.mydestiny.domain.ProfilePhoto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchingResponse(
        String id,
        ProfileSummary requesterProfile,
        ProfileSummary targetProfile,
        String requesterNickname,
        String receiverNickname,
        String status,
        String message,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime receiverRespondedAt,
        LocalDateTime receiverExpiresAt
) {
    public record ProfileSummary(String id, String name, String gender, List<String> photoUrls) {}

    public static MatchingResponse from(Matching m) {
        return new MatchingResponse(
                m.getId(),
                toSummary(m.getRequesterProfile()),
                toSummary(m.getTargetProfile()),
                m.getRequester().getNickname(),
                m.getReceiver().getNickname(),
                m.getStatus().name(),
                m.getMessage(),
                m.getRejectReason(),
                m.getCreatedAt(),
                m.getReceiverRespondedAt(),
                m.getReceiverExpiresAt()
        );
    }

    private static ProfileSummary toSummary(DatingProfile p) {
        return new ProfileSummary(
                p.getId(),
                p.getName(),
                p.getGender() != null ? p.getGender().getDbValue() : null,
                p.getPhotos().stream().map(ProfilePhoto::getImageUrl).toList()
        );
    }
}
