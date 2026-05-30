package com.mydestiny.dto.profile;

import com.mydestiny.domain.DatingProfile;

import java.time.LocalDateTime;

public record ProfileSummaryResponse(
        String id,
        String name,
        String status,
        String visibility,
        String firstPhotoUrl,
        LocalDateTime createdAt
) {
    public static ProfileSummaryResponse from(DatingProfile p) {
        String firstPhoto = p.getPhotos().isEmpty()
                ? null
                : p.getPhotos().get(0).getImageUrl();

        return new ProfileSummaryResponse(
                p.getId(),
                p.getName(),
                p.getStatus().name(),
                p.getVisibility().name(),
                firstPhoto,
                p.getCreatedAt()
        );
    }
}
