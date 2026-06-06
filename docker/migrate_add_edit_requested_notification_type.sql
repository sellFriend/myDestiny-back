-- =====================================================
-- Migration: notifications.type ENUM에 'edit_requested' 값 추가
-- 주선자가 친구의 폼 카드를 "수정 요청" 했을 때 친구에게 발송하는 알림.
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_edit_requested_notification_type.sql
-- =====================================================

ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'form_submitted',
        'match_request',
        'match_accepted',
        'match_rejected',
        'verification_done',
        'acquaintance_blocked',
        'match_consent_requested',
        'match_counterpart_consented',
        'match_consent_rejected',
        'matched',
        'match_request_expired',
        'match_consent_expired',
        'edit_requested'
    ) NOT NULL;
