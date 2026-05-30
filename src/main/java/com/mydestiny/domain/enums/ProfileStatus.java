package com.mydestiny.domain.enums;

public enum ProfileStatus {
    DRAFT,            // 초안 (초대 전)
    INVITED,          // 초대 링크 발송됨
    PENDING_APPROVAL, // B 로그인·번호 인증 완료, 확인 중
    APPROVED,         // B 승인 (즉시 공개 전 단계)
    REVIEW_REQUIRED,  // 등록자=승인자 감지, 관리자 검수 대기
    PUBLISHED,        // 최종 공개
    REJECTED,         // B 거절 또는 관리자 거절
    REPORTED,         // 신고 접수
    SUSPENDED,        // 관리자 숨김
    DELETED           // 삭제 (soft)
}
