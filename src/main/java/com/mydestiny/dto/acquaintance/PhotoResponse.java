package com.mydestiny.dto.acquaintance;

import com.mydestiny.domain.ProfilePhoto;

public record PhotoResponse(String id, String url, int displayOrder) {

    public static PhotoResponse from(ProfilePhoto photo) {
        return new PhotoResponse(photo.getId(), photo.getImageUrl(), photo.getDisplayOrder());
    }
}
