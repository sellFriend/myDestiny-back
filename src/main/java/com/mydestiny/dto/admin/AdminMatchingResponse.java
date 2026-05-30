package com.mydestiny.dto.admin;

import com.mydestiny.domain.Matching;

import java.time.LocalDateTime;

public record AdminMatchingResponse(
        String id,
        String status,
        String requesterNickname,
        String requesterProfileName,
        String receiverNickname,
        String targetProfileName,
        String message,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {
    public static AdminMatchingResponse from(Matching m) {
        return new AdminMatchingResponse(
                m.getId(),
                m.getStatus().name(),
                m.getRequester().getNickname(),
                m.getRequesterProfile().getName(),
                m.getReceiver().getNickname(),
                m.getTargetProfile().getName(),
                m.getMessage(),
                m.getRejectReason(),
                m.getCreatedAt(),
                m.getReceiverRespondedAt()
        );
    }
}
