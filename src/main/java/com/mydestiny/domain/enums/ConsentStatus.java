package com.mydestiny.domain.enums;

public enum ConsentStatus {
    PENDING,   // 동의 요청 발송됨, 미응답
    APPROVED,  // 동의함
    REJECTED,  // 거절함
    EXPIRED    // 기한 내 미응답
}
