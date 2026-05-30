package com.mydestiny.domain.enums;

public enum MatchingStatus {
    PENDING,                     // 요청자가 요청 전송, 수신자 미응답
    CANCELLED,                   // 요청자가 취소 (PENDING에서만 가능)
    EXPIRED,                     // 수신자 응답 기한 초과
    REJECTED_BY_RECEIVER,        // 수신자가 거절
    ACCEPTED_BY_RECEIVER,        // 수신자가 수락 (CONSENT_PENDING 전 경유 상태)
    CONSENT_PENDING,             // 당사자 양쪽 동의 대기 중
    CONSENT_PARTIALLY_APPROVED,  // 한 명만 동의 완료
    CONSENT_REJECTED,            // 당사자 중 한 명 거절
    CONSENT_EXPIRED,             // 당사자 동의 기한 초과
    MATCHED                      // 양쪽 모두 동의, 매칭 성사
}
