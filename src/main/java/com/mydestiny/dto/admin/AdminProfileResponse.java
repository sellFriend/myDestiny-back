package com.mydestiny.dto.admin;

import com.mydestiny.domain.DatingProfile;

import java.time.LocalDateTime;

public record AdminProfileResponse(
        String id,
        String status,
        String registrantNickname,
        String subjectName,
        boolean isSamePersonDetected,
        LocalDateTime createdAt
) {
    public static AdminProfileResponse from(DatingProfile p) {
        return new AdminProfileResponse(
                p.getId(),
                p.getStatus().name(),
                p.getRegistrant().getNickname(),
                p.getName(),
                p.isSamePersonDetected(),
                p.getCreatedAt()
        );
    }
}
