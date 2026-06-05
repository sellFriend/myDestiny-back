package com.mydestiny.dto.acquaintance;

import com.mydestiny.domain.AcquaintancePhoto;

public record PhotoResponse(String id, String url, int displayOrder) {

    public static PhotoResponse from(AcquaintancePhoto photo) {
        return new PhotoResponse(photo.getId(), photo.getImageUrl(), photo.getDisplayOrder());
    }
}
