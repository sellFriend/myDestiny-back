package com.mydestiny.dto.admin;

import com.mydestiny.domain.Report;

import java.time.LocalDateTime;

public record AdminReportResponse(
        String id,
        String profileId,
        String profileName,
        String reporterNickname,
        String reason,
        String detail,
        String status,
        LocalDateTime createdAt
) {
    public static AdminReportResponse from(Report r) {
        return new AdminReportResponse(
                r.getId(),
                r.getProfile().getId(),
                r.getProfile().getName(),
                r.getReporter().getNickname(),
                r.getReason(),
                r.getDetail(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
