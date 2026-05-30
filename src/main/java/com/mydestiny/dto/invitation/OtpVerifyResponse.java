package com.mydestiny.dto.invitation;

public record OtpVerifyResponse(
        String profileId,
        String newStatus  // "PENDING_APPROVAL" or "REVIEW_REQUIRED"
) {}
