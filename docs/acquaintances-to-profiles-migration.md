# 주선자 폼 시스템 통합 — acquaintances → dating_profiles

> 작성일: 2026-06-06
> 대상: 프론트엔드 팀
> 백엔드 브랜치: `hs/work`

## 배경

기존엔 두 시스템이 분리돼 있었습니다.

- **구 `acquaintances`** — 주선자 폼/승인/카드/차단
- **신 `dating_profiles`** — 매칭용 프로필

둘이 연결되지 않아 폼으로 승인한 친구가 매칭 풀에 들어가지 않았습니다. 이번 작업으로 **저장소를 `dating_profiles` 하나로 통합**했습니다. 이제 주선자가 친구를 승인하면(`PUBLISHED`) 곧바로 카드/매칭에 노출됩니다.

---

## 프론트 영향 ① — status 값 변경 (필수 대응)

응답의 **status 문자열 값**이 바뀝니다. URL·요청 바디·응답 필드 이름은 그대로입니다.

| 구 값 | 신 값 | 의미 |
|---|---|---|
| `draft` | `DRAFT` | 작성 중 |
| `verification_pending` | `PENDING_APPROVAL` | **승인 대기 (내 친구 탭)** |
| `verified` | `PUBLISHED` | 승인 완료 (카드/매칭 노출) |

### 영향받는 응답

| 엔드포인트 | 필드 |
|---|---|
| `GET /api/acquaintances` | `data[].registrationStatus` |
| `GET /api/acquaintances/{id}` | `data.registrationStatus` |
| `GET /form/{madamId}` | `data.draft.status` |
| `POST /form/{madamId}` | `data.status` |

### 대응

- "승인 대기" 탭 필터: `status === 'verification_pending'` → `status === 'PENDING_APPROVAL'`
- 승인 완료 표시: `'verified'` → `'PUBLISHED'`
- 작성 중: `'draft'` → `'DRAFT'`

---

## 프론트 영향 ② — 중복 차단 (변경 없음, 유지됨)

폼 제출 시 중복 차단은 그대로 유지됩니다. 둘 다 `409 CONFLICT`.

| 케이스 | 메시지 | 기준 |
|---|---|---|
| 같은 계정이 다른 주선자에게 또 등록 | `이미 다른 주선자를 통해 등록되어 있습니다.` | `subject_id` |
| 같은 번호가 이미 승인 완료(PUBLISHED) | `이미 다른 주선자를 통해 등록 완료된 번호입니다.` | 전화번호 blind index |

> 이메일 기준 중복 차단만 제거됐습니다(원래 호출되지 않던 데드 체크).
>
> 전화번호 중복은 blind index(고정키 HMAC-SHA256, `subject_phone_lookup`)로 복원했습니다. 같은 번호는 항상 같은 인덱스 값을 내므로 다른 카카오 계정 + 같은 번호 조합도 차단됩니다.

---

## 프론트 영향 ③ — `/api/acquaintances` 엔드포인트 이전 (필수 대응)

`/api/acquaintances/**`는 **제거**되고 `/api/profiles/**`로 이전됐습니다 (클린 컷). 상세 정리는 [profiles-vs-acquaintances-api-roles.md](./profiles-vs-acquaintances-api-roles.md) 참고.

| 구 (제거됨) | 신 | 응답 변화 |
|---|---|---|
| `GET /api/acquaintances` | `GET /api/profiles` | `AcquaintanceDetailResponse[]` (동일 DTO) |
| `GET /api/acquaintances/{id}` | `GET /api/profiles/{id}` | **`ProfileDetailResponse`로 변경** (필드 더 많음) |
| `POST /api/acquaintances/{id}/approve` | `POST /api/profiles/{id}/approve` | – |
| `POST /api/acquaintances/{id}/reject` | `POST /api/profiles/{id}/reject` | – |
| `POST /api/acquaintances/{id}/request-edit` | `POST /api/profiles/{id}/request-edit` | – |
| `GET /api/acquaintances/my-form` | `GET /api/profiles/my-form` | – |

> ⚠️ `GET /api/profiles`는 기존에 `ProfileSummaryResponse[]`를 반환했으나, 이제 **주선자 친구 목록(`AcquaintanceDetailResponse[]`)**을 반환합니다. 이 엔드포인트를 쓰던 화면이 있으면 응답 구조 변경 주의.

### 응답 모양

**`GET /api/profiles` — 친구 목록 (`AcquaintanceDetailResponse[]`)**

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "uuid",
      "name": "김지인",
      "age": 27,
      "gender": "female",
      "job": "디자이너",
      "intro": "안녕하세요!",
      "mbti": "INFJ",
      "hobbies": "독서, 요가",
      "registrationStatus": "PENDING_APPROVAL",
      "verifiedAt": null,
      "photoUrls": ["https://cdn.example.com/photos/1.jpg"]
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `registrationStatus` | string | `ProfileStatus` — `DRAFT` / `PENDING_APPROVAL` / `PUBLISHED` 등 |
| `verifiedAt` | string 또는 null | 승인(`PUBLISHED`) 시각. 미승인 시 `null` |
| `photoUrls` | string[] | `displayOrder` 오름차순 |

**`GET /api/profiles/{id}` — 상세 (`ProfileDetailResponse`)**

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "registrantId": "uuid",
    "registrantNickname": "주선자닉네임",
    "status": "PUBLISHED",
    "name": "김지인",
    "age": 27,
    "gender": "female",
    "isStudent": false,
    "schoolName": null,
    "major": null,
    "occupation": "디자이너",
    "mbti": "INFJ",
    "hobby": "독서, 요가",
    "introduction": "안녕하세요!",
    "kakaoId": "kakao_abc",
    "instagramId": "insta_abc",
    "subjectPhone": "010****5678",
    "photoUrls": ["https://cdn.example.com/photos/1.jpg"],
    "createdAt": "2026-06-01T10:00:00",
    "updatedAt": "2026-06-05T10:00:00"
  }
}
```

> ⚠️ 필드 이름 차이 주의: 상세는 목록과 달리 `job→occupation`, `intro→introduction`, `hobbies→hobby`, `registrationStatus→status` 입니다.
> `kakaoId` / `instagramId` / `subjectPhone`은 **등록자(주선자) 본인 조회 시에만** 값이 오고, 그 외(당사자/열람자)는 `null`.

---

## 변경 없음 (그대로 사용)

- URL: `/form/**`, `/api/cards/**`, `/api/blocks/**`
- 요청 바디: `FormDataRequest`, `BlockRequest` 필드 동일
- `uploadToken` 발급 및 사진 업로드 흐름(`POST/PUT /form/{uploadToken}/photos`) 동일

## 변경됨 — 사진 장수 제한

친구 1명당 사진 **최대 5장 → 1장**으로 변경 (`MAX_PHOTOS` 상수). 추후 여러 장으로 확장 가능.

---

## 백엔드/DB 변경 (참고)

### 삭제

- 엔티티/레포: `Acquaintance`, `AcquaintancePhoto` 및 각 Repository
- enum: `RegistrationStatus`, `Visibility`

### dating_profiles로 전환

- 폼 제출 → `DatingProfile` 생성 (registrant=주선자, subject=친구, status=`PENDING_APPROVAL`)
- 전화번호 → 폼 입력값을 `hash`(검증용) + `encrypt`(표시용)로 저장, SMS 인증은 생략
- 카드 → `PUBLISHED` + `PUBLIC` 프로필 조회
- 차단 → `blocks` FK를 `dating_profiles`로 repoint

### 마이그레이션 (원격 DB 적용 순서)

1. `docker/migrate_add_dating_profiles_upload_token.sql` (`upload_token` + `subject_phone_lookup` 컬럼 추가)
2. `docker/migrate_repoint_blocks_to_dating_profiles.sql` (기존 blocks 데이터 정리 포함)
3. `docker/migrate_drop_acquaintances_tables.sql`

> 운영 환경엔 `PHONE_LOOKUP_KEY` 환경변수(base64 인코딩, 16바이트 이상)를 반드시 설정해야 합니다. blind index HMAC 키이며, 한번 정하면 바꾸면 안 됩니다(바꾸면 기존 저장값과 조회 불일치).

---

## 남은 정리거리 (선택)

URL `/api/acquaintances`는 제거됐습니다. 다만 내부 클래스명(`AcquaintanceService`, `AcquaintanceDetailResponse`, `AcquaintanceStatusFilter`)·DTO 패키지(`dto/acquaintance`)에는 "acquaintance"가 남아 있습니다. 기능엔 영향 없으며, 추후 `friend`/`profile` 계열로 리네이밍 가능합니다. (`ProfileController`가 `AcquaintanceService`를 주입받는 형태라 리네이밍하면 더 일관됨)
