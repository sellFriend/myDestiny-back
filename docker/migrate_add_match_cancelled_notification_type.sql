-- =====================================================
-- Migration: notifications.type ENUM에 'match_cancelled' 값 추가
-- 매칭 성사 시, 두 프로필에 엮인 다른 매칭 요청이 자동 취소되면서 양측에 발송하는 알림.
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_match_cancelled_notification_type.sql
-- 원격 앱 DB 실행: mariadb -h 138.2.98.160 -P 3307 -u root -proot my_destiny < docker/migrate_add_match_cancelled_notification_type.sql
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
        'edit_requested',
        'match_cancelled'
    ) NOT NULL;
