# `/api/acquaintances` vs `/api/profiles` — 역할 정리 및 통합 제안

> 작성일: 2026-06-06
> 대상: 백엔드/프론트엔드 팀
> 배경: 두 API가 이제 **같은 `dating_profiles` 테이블**을 사용 → 역할 정리 후 `/api/acquaintances`를 `/api/profiles`로 흡수
>
> ✅ **상태: 흡수 완료 (2026-06-06, 클린 컷).** `/api/acquaintances`는 제거됨. 아래 2~5절은 통합 전 분석 기록이며, 최종 결과는 [6절](#6-통합-제안--apiacquaintances--apiprofiles-흡수) 참조.

---

## 1. 큰 그림

저장소 통합 후, 두 컨트롤러는 **동일한 `dating_profiles`를 서로 다른 관점에서** 다룹니다.

| 관점 | 컨트롤러 | 주체 | 한 줄 역할 |
|---|---|---|---|
| 주선자 폼 흐름 | `AcquaintanceController` | 주선자(registrant) | 내가 등록한 친구 카드 관리 (목록·승인·거절·수정요청) |
| 프로필 CRUD/공개 | `ProfileController` | 등록자/당사자/열람자 | 프로필 생성·수정·삭제·공개조회·사진·신고 |
| 친구 작성 | `FormController` (`/form/**`) | 친구(subject) | 폼 진입·제출·사진 업로드 |

핵심: **`dating_profiles` 한 행 = 한 명의 친구 프로필**. 그 행을 "주선자가 관리"하면 `/api/acquaintances`, "프로필 자체를 다루면" `/api/profiles`.

---

## 2. `/api/acquaintances` — 주선자 폼 흐름

> 담당: `AcquaintanceController` → `AcquaintanceService`
> 인증: 전부 필요 (주선자 본인)

| 메서드 | 경로 | 역할 | 응답 |
|---|---|---|---|
| GET | `/api/acquaintances` | 내가 등록한 친구 카드 목록 (DELETED 제외) | `AcquaintanceDetailResponse[]` |
| GET | `/api/acquaintances/my-form` | 내 영구 폼 링크 조회 (매물 등록자는 403) | `InviteResponse` |
| GET | `/api/acquaintances/{id}` | 내 친구 카드 상세 | `AcquaintanceDetailResponse` |
| POST | `/api/acquaintances/{id}/approve` | 승인 → `PUBLISHED` (매칭 노출) | – |
| POST | `/api/acquaintances/{id}/reject` | 거절 → soft delete | – |
| POST | `/api/acquaintances/{id}/request-edit` | 수정 요청 → `DRAFT` 되돌림 + 친구 알림 | – |

`AcquaintanceDetailResponse`: `id, name, age, gender, job, intro, mbti, hobbies, registrationStatus, verifiedAt, photoUrls`
※ `job`=occupation, `intro`=introduction, `hobbies`=hobby, `registrationStatus`=ProfileStatus, `verifiedAt`=publishedAt 매핑

**상태 흐름:** 친구 폼 제출(`/form`) → `PENDING_APPROVAL` → 주선자 `approve` → `PUBLISHED`

---

## 3. `/api/profiles` — 프로필 CRUD / 공개

> 담당: `ProfileController` → `ProfileService`
> 인증: 전부 필요

| 메서드 | 경로 | 역할 | 응답 |
|---|---|---|---|
| POST | `/api/profiles` | 프로필 생성 (등록자가 당사자 번호 입력 = 초대 흐름) | `ProfileDetailResponse` |
| GET | `/api/profiles` | 내가 등록한 프로필 목록 (DELETED 제외) | `ProfileSummaryResponse[]` |
| GET | `/api/profiles/public` | 공개 프로필 목록 (registrantId·gender 필터) | `PublicProfileResponse[]` |
| GET | `/api/profiles/{id}` | 프로필 상세 (소유자/당사자/열람자별 노출 차등) | `ProfileDetailResponse` |
| PATCH | `/api/profiles/{id}` | 등록자 수정 → `DRAFT` 재초기화 | `ProfileDetailResponse` |
| DELETE | `/api/profiles/{id}` | 삭제 (등록자/당사자) | – |
| POST | `/api/profiles/{id}/photos` | 사진 업로드 (MVP 1장) | `String(url)` |
| DELETE | `/api/profiles/{id}/photos/{photoId}` | 사진 삭제 | – |
| PATCH | `/api/profiles/{id}/visibility` | 공개 범위 변경 | – |
| POST | `/api/profiles/{id}/reports` | 프로필 신고 | – |

`ProfileSummaryResponse`: `id, name, status, visibility, firstPhotoUrl, createdAt`
`ProfileDetailResponse`: 위 + `registrantId, registrantNickname, isStudent, schoolName, major, occupation, kakaoId, instagramId, subjectPhone(등록자만), createdAt, updatedAt`
`PublicProfileResponse`: `id, name, age, gender, isStudent, schoolName, major, occupation, mbti, hobby, introduction, firstPhotoUrl`

**상태 흐름(초대):** 생성 → 초대 발송(`/api/profiles/{id}/invite`) → 친구 번호 인증 → `PENDING_APPROVAL` → **친구 본인** 승인(`ApprovalService`, 토큰) → `PUBLISHED`

---

## 4. 두 흐름의 차이 (같은 테이블, 다른 진입)

| | 주선자 폼 흐름 (`/api/acquaintances`+`/form`) | 초대 흐름 (`/api/profiles`) |
|---|---|---|
| 데이터 입력자 | **친구 본인** (폼 작성) | **등록자** (당사자 번호 입력) |
| 승인자 | **주선자** | **친구 본인** (번호 인증 후) |
| 친구 연결 시점 | 폼 제출 시 (카카오 로그인) | 번호 인증 시 (`linkSubject`) |
| 초대/번호인증 | 없음 (폼 링크) | 있음 (`invitations`, `phone_verifications`) |
| 도달 상태 | `PENDING_APPROVAL` → `PUBLISHED` | 동일 |

→ **둘 다 결국 `PUBLISHED` 프로필**이 되어 `/api/cards`·매칭 풀에 합류.

---

## 5. 중복/혼선 지점

저장소 통합으로 아래가 **사실상 같은 동작**이 됐습니다.

| 중복 | `/api/acquaintances` | `/api/profiles` | 비고 |
|---|---|---|---|
| 내 등록 목록 | `GET /api/acquaintances` | `GET /api/profiles` | **동일 쿼리** (registrant + status≠DELETED), DTO만 다름 |
| 상세 조회 | `GET /api/acquaintances/{id}` | `GET /api/profiles/{id}` | 권한·노출 범위 로직이 다름 (acquaintance는 등록자 전용) |
| 사진 | `/form/{token}/photos` (친구, 5장) | `/api/profiles/{id}/photos` (등록자, 1장) | 업로더·장수 제한 불일치 |

승인 주체도 둘로 갈림: 주선자 승인(`AcquaintanceController`) vs 친구 승인(`ApprovalService`).

---

## 6. 통합 제안 — `/api/acquaintances` → `/api/profiles` 흡수

### 매핑

| 현재 | 제안 |
|---|---|
| `GET /api/acquaintances` | `GET /api/profiles` (이미 존재) — DTO를 `ProfileSummaryResponse`로 일원화하거나 `?view=registrant` 제공 |
| `GET /api/acquaintances/{id}` | `GET /api/profiles/{id}` (이미 존재) |
| `POST /api/acquaintances/{id}/approve` | `POST /api/profiles/{id}/approve?by=registrant` 또는 신규 `/api/profiles/{id}/registrant-approval` |
| `POST /api/acquaintances/{id}/reject` | `DELETE /api/profiles/{id}` (soft delete) 와 통합 |
| `POST /api/acquaintances/{id}/request-edit` | `POST /api/profiles/{id}/request-edit` |
| `GET /api/acquaintances/my-form` | `GET /api/profiles/my-form` (또는 `/api/registrants/me/form`) |

### 확정된 정책 (2026-06-06)

1. **승인 주체 = 주선자(registrant).** 통합 엔드포인트의 승인은 주선자가 수행한다. (초대 흐름의 친구-본인 승인 `ApprovalService`는 별개로 유지)
2. **목록/상세 DTO = `AcquaintanceDetailResponse`.** 주선자 관점 목록은 이 DTO로 일원화. (`ProfileSummaryResponse`는 사용처가 사라지면 제거 대상)
3. **사진 = 1장.** 현재는 1장으로 제한하되, 장수 제한을 상수/설정으로 두어 **추후 여러 장 확장이 한 줄 변경**이 되도록 구조화한다.

### 통합 결과 (✅ 적용 완료 — 클린 컷, `/api/acquaintances` 제거됨)

| 구 (`/api/acquaintances`, 제거됨) | 신 (`/api/profiles`) | 응답 |
|---|---|---|
| `GET /api/acquaintances` | `GET /api/profiles` (주선자 목록) | `AcquaintanceDetailResponse[]` |
| `GET /api/acquaintances/{id}` | `GET /api/profiles/{id}` | **`ProfileDetailResponse`** (역할별 필드 마스킹) |
| `POST /api/acquaintances/{id}/approve` | `POST /api/profiles/{id}/approve` | – |
| `POST /api/acquaintances/{id}/reject` | `POST /api/profiles/{id}/reject` | – |
| `POST /api/acquaintances/{id}/request-edit` | `POST /api/profiles/{id}/request-edit` | – |
| `GET /api/acquaintances/my-form` | `GET /api/profiles/my-form` | `InviteResponse` |

- 상세(`GET /api/profiles/{id}`)는 **전부 `ProfileDetailResponse`로 통일** — 소유자/당사자/열람자별로 `from`/`fromPublic`이 필드를 차등 노출.
- 목록(`GET /api/profiles`)은 **`AcquaintanceDetailResponse[]`** (주선자 관점). 기존 `ProfileSummaryResponse`는 제거됨.
- 사진 업로드(친구단)는 `/form/**` 그대로 유지, 장수 제한 = 1 (`MAX_PHOTOS` 상수, 추후 확장 시 값만 변경).
- 승인 = 주선자(registrant). 초대 흐름의 친구-본인 승인(`ApprovalService`)은 별개 유지.

---

## 부록 — 관련 API (참고)

| 컨트롤러 | 경로 | 역할 |
|---|---|---|
| `FormController` | `/form/**` | 친구 폼 진입·제출·사진 (주선자 폼 흐름의 입력단) |
| `CardController` | `/api/cards` | `PUBLISHED`+`PUBLIC` 카드 목록/상세 |
| `BlockController` | `/api/blocks` | 프로필 차단/해제 |
| `InvitationController` | `/api/invitations`, `/api/profiles/{id}/invite` | 초대 발송/조회 (초대 흐름) |
| `PhoneVerificationController` | – | 친구 번호 인증 (초대 흐름) |
| `MatchingController` / `CandidateConsentController` | `/api/matchings` 등 | 매칭 요청·동의 |
| `RegistrantController` | `/api/registrants` | 주선자 프로필·팔로우 |
