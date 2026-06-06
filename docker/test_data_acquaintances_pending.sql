-- =====================================================
-- Test Data: 승인 대기 탭 테스트 데이터 5건 (dating_profiles)
-- registrant_id: 73ebdb1e-bc01-4a04-bb10-23604f89f0df
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/test_data_acquaintances_pending.sql
-- =====================================================

INSERT INTO dating_profiles (
    id, registrant_id, subject_id,
    status, name, age, gender,
    is_student, occupation, mbti, hobby, introduction,
    kakao_id, instagram_id,
    visibility,
    subject_phone_hash, subject_phone_encrypted,
    is_same_person_detected,
    created_at, updated_at
) VALUES
(
    UUID(), '73ebdb1e-bc01-4a04-bb10-23604f89f0df', NULL,
    'PENDING_APPROVAL', '김민준', 27, 'MALE',
    0, '소프트웨어 개발자', 'INTJ', '등산, 요리, 독서',
    '취미로 등산과 요리를 즐기는 평범한 직장인입니다.',
    'minjun_k', 'minjun.kim',
    'PUBLIC',
    'test_hash_010-1111-0001', NULL,
    0,
    NOW(), NOW()
),
(
    UUID(), '73ebdb1e-bc01-4a04-bb10-23604f89f0df', NULL,
    'PENDING_APPROVAL', '이서연', 25, 'FEMALE',
    0, '간호사', 'ENFJ', '카페 탐방, 영화 감상, 요가',
    '활발하고 긍정적인 성격입니다. 카페 탐방을 좋아해요.',
    'seoyeon_l', NULL,
    'PUBLIC',
    'test_hash_010-2222-0002', NULL,
    0,
    NOW(), NOW()
),
(
    UUID(), '73ebdb1e-bc01-4a04-bb10-23604f89f0df', NULL,
    'PENDING_APPROVAL', '박지호', 29, 'MALE',
    0, '디자이너', 'INFP', '전시회 관람, 음악 감상, 그림',
    '감성적이고 창의적인 사람입니다. 전시회나 공연 보는 걸 좋아해요.',
    NULL, 'jiho_design',
    'PUBLIC',
    'test_hash_010-3333-0003', NULL,
    0,
    NOW(), NOW()
),
(
    UUID(), '73ebdb1e-bc01-4a04-bb10-23604f89f0df', NULL,
    'PENDING_APPROVAL', '최수아', 26, 'FEMALE',
    0, '마케터', 'ESTP', '헬스, 러닝, 맛집 탐방',
    '새로운 사람 만나는 걸 좋아하고 운동을 꾸준히 합니다.',
    'sua_choi', 'sua_choi',
    'PUBLIC',
    'test_hash_010-4444-0004', NULL,
    0,
    NOW(), NOW()
),
(
    UUID(), '73ebdb1e-bc01-4a04-bb10-23604f89f0df', NULL,
    'PENDING_APPROVAL', '정현우', 28, 'MALE',
    0, '교사', 'ISTJ', '독서, 보드게임, 자전거',
    '차분하고 배려심 있는 성격입니다. 주말엔 보통 독서나 게임을 해요.',
    'hyunwoo_j', NULL,
    'PUBLIC',
    'test_hash_010-5555-0005', NULL,
    0,
    NOW(), NOW()
);
