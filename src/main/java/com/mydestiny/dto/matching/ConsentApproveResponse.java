package com.mydestiny.dto.matching;

public record ConsentApproveResponse(
        String consentStatus,   // APPROVED
        String matchingStatus   // CONSENT_PARTIALLY_APPROVED | MATCHED
) {}
