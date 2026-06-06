# My Destiny — 프론트엔드 개발 요청 문서

> 작성일: 2026-05-30  
> 대상: 프론트엔드 팀  
> 백엔드 브랜치: `hs/docker`

**Base URL**
| 환경 | URL |
|---|---|
| 로컬 | `http://localhost:8888/destiny` |

---

## 목차

1. [서비스 개요 및 핵심 개념](#1-서비스-개요-및-핵심-개념)
2. [인증 흐름](#2-인증-흐름)
3. [공통 응답 형식](#3-공통-응답-형식)
4. [Enum 타입 정의](#4-enum-타입-정의)
5. [API 명세](#5-api-명세)
   - [인증 (Auth)](#51-인증-auth)
   - [사용자 (User)](#52-사용자-user)
   - [주선자 (Registrant)](#53-주선자-registrant)
   - [팔로우 (Follow)](#54-팔로우-follow)
   - [지인 프로필 등록 — 폼 (Form)](#55-지인-프로필-등록--폼-form)
   - [지인 관리 (Acquaintance)](#56-지인-관리-acquaintance)
   - [데이팅 프로필 (Profile)](#57-데이팅-프로필-profile)
   - [초대 링크 — 당사자 승인 흐름 (Invitation)](#58-초대-링크--당사자-승인-흐름-invitation)
   - [카드 (Card)](#59-카드-card)
   - [매칭 (Matching)](#510-매칭-matching)
   - ~~당사자 동의 (Candidate Consent)~~ — 폐지
   - [전화번호 인증 (Phone Verification)](#512-전화번호-인증-phone-verification)
   - [알림 (Notification)](#513-알림-notification)
   - [차단 (Block)](#514-차단-block)
   - [관리자 (Admin)](#515-관리자-admin)

---

## 1. 서비스 개요 및 핵심 개념

My Destiny는 **주선자(Registrant)** 가 자신의 지인을 소개팅 후보로 등록하고, 다른 주선자의 지인과 매칭을 연결해주는 중개 서비스입니다.

### 역할 관계

```
주선자 A (requester)        주선자 C (receiver)
    |                             |
  지인 B (requesterProfile)   지인 D (targetProfile)
```

- **주선자 (A / C)**: 서비스에 가입한 일반 사용자. 자신의 지인을 등록하고 매칭을 요청/수락합니다.
- **지인 (Acquaintance, B / D)**: 주선자가 등록한 소개팅 후보. 별도 계정이 없고, 폼 링크나 초대 링크로 정보를 입력합니다.
- **DatingProfile**: 지인의 공개 소개팅 프로필.

### 전체 매칭 흐름

```
[A] 지인 B 등록 (폼 링크 전달 또는 직접 프로필 생성)
        ↓
[A] B의 프로필 초대 링크 생성 → [B] 링크로 접근, 개인정보 동의·승인
        ↓
[A] C의 지인 D와 매칭 요청 → status: PENDING        (C에게 match_request 알림)
        ↓
[C] 수락 → status: MATCHED (즉시 성사)               (A·C 양쪽에 matched 알림)
   또는 [C] 거절 → status: REJECTED_BY_RECEIVER       (A에게 match_rejected 알림)
   또는 [A] 취소 → status: CANCELLED (PENDING 한정)
        ↓
매칭 성사 → 서로 연락처(카카오ID / 인스타ID) 공개
```

---

## 2. 인증 흐름

### 소셜 로그인 (카카오 OAuth2)

1. 프론트에서 `GET /destiny/oauth2/authorization/kakao` 로 리다이렉트
2. 로그인 완료 후 서버가 아래 URL로 리다이렉트:
   ```
   {REDIRECT_URI}?accessToken={accessToken}&refreshToken={refreshToken}&userId={userId}&profileImageUrl={url}
   ```
   > 기본 redirect URI: `http://localhost:3000/oauth2/callback` (환경변수 `FRONTEND_URL`로 변경 가능)  
   > `userId`: 로그인한 사용자의 식별자(UUID). 항상 포함.  
   > `profileImageUrl`: 카카오 프로필 사진 URL. 없으면 파라미터 미포함.
3. 프론트는 쿼리 파라미터를 파싱해 토큰을 저장합니다.
4. 폼 링크를 통해 진입한 경우: 로그인 완료 후 `profileImageUrl` 존재 시 "카카오 프로필 사진을 폼에 사용할까요?" 팝업 표시.

### 토큰 사용

모든 인증 필요 API 요청에 헤더 추가:
```
Authorization: Bearer {accessToken}
```

### 인증 불필요 (Public) 엔드포인트

| 경로 | 설명 |
|------|------|
| `GET /destiny/oauth2/**`, `GET /destiny/login/**` | OAuth2 로그인 |
| `POST /destiny/api/auth/refresh` | 토큰 갱신 |
| `GET /destiny/api/invitations/{token}` | 초대 링크 정보 조회 |
| `GET /destiny/form/{madamId}` | 폼 링크 유효성 확인 |

> `POST /destiny/form/{madamId}` (폼 제출)와 `POST /destiny/form/{uploadToken}/photos` (사진 업로드)는 **카카오 로그인 후 JWT 필요**.

### 토큰 갱신

Access Token 만료 시 Refresh Token으로 재발급:
```
POST /destiny/api/auth/refresh
X-Refresh-Token: {refreshToken}
```

---

## 3. 공통 응답 형식

모든 API는 아래 형식으로 응답합니다:

```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | `boolean` | 성공 여부 |
| `message` | `string` | 메시지 (에러 시 설명 포함) |
| `data` | `T \| null` | 응답 데이터 |

### 에러 응답 예시

```json
{
  "success": false,
  "message": "해당 프로필을 찾을 수 없습니다.",
  "data": null
}
```

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "로그인이 필요합니다."
}
```

---

## 4. Enum 타입 정의

### ProfileStatus (프로필 상태)

| 값 | 설명 |
|----|------|
| `DRAFT` | 초안 (초대 전) |
| `INVITED` | 초대 링크 발송됨 |
| `PENDING_APPROVAL` | 당사자 로그인·번호 인증 완료, 검토 중 |
| `APPROVED` | 당사자 승인 (공개 전 단계) |
| `REVIEW_REQUIRED` | 등록자=승인자 감지, 관리자 검수 대기 |
| `PUBLISHED` | 최종 공개 상태 |
| `REJECTED` | 당사자 또는 관리자 거절 |
| `REPORTED` | 신고 접수 |
| `SUSPENDED` | 관리자 숨김 |
| `DELETED` | 삭제 (soft delete) |

### ProfileVisibility (공개 범위)

| 값 | 설명 |
|----|------|
| `PUBLIC` | 전체 공개 |
| `FOLLOWERS_ONLY` | 팔로워 공개 |
| `PRIVATE` | 비공개 |

### MatchingStatus (매칭 상태)

> ℹ️ 수신자(C)가 수락하면 **즉시 `MATCHED`** 가 됩니다. 프론트는 아래 **사용 중** 5개 상태만 다루면 됩니다.

| 값 | 설명 |
|----|------|
| `PENDING` | 요청 전송, 수신자 미응답 |
| `CANCELLED` | 요청자가 취소 |
| `EXPIRED` | 수신자 응답 기한 초과 |
| `REJECTED_BY_RECEIVER` | 수신자(C)가 거절 |
| `MATCHED` | 수신자(C) 수락 → 매칭 성사 |

> 아래 상태들은 과거 당사자 동의(2단계) 흐름 잔재로 **현재 사용되지 않습니다** (수락 즉시 성사로 변경).
> 응답에 등장하지 않으므로 프론트에서 처리할 필요가 없습니다.
> `ACCEPTED_BY_RECEIVER`, `CONSENT_PENDING`, `CONSENT_PARTIALLY_APPROVED`, `CONSENT_REJECTED`, `CONSENT_EXPIRED`

### ~~ConsentStatus (당사자 동의 상태)~~ — 미사용

> 당사자 동의 흐름이 폐지되어 더 이상 노출되지 않습니다. (수락 즉시 성사)

### ~~RegistrationStatus (지인 등록 상태)~~ — 폐지, ProfileStatus로 통합

> ⚠️ **2026-06-06 변경**: 지인(폼) 시스템이 `dating_profiles`로 통합되면서 별도 `RegistrationStatus`가 사라졌습니다.
> 폼/지인 관련 모든 응답의 상태값은 이제 **`ProfileStatus`**(위 표)를 사용합니다.

| 구 값 (폐지) | 신 값 (ProfileStatus) | 의미 |
|----|----|----|
| `draft` | `DRAFT` | 폼 미제출/수정 대기 |
| `verification_pending` | `PENDING_APPROVAL` | 폼 제출 완료, 주선자 승인 대기 |
| `verified` | `PUBLISHED` | 주선자 승인 완료 (카드/매칭 노출) |

### NotificationType (알림 타입)

| 값 | 발송 시점 | 받는 사람 |
|----|-----------|-----------|
| `match_request` | A가 매칭 요청 생성 | 수신자 C |
| `matched` | C가 수락 → 즉시 성사 | A·C 양쪽 |
| `match_rejected` | C가 거절 | 요청자 A |
| `match_request_expired` | 수신자 응답 기한(72h) 만료 | — |
| `form_submitted` | 지인이 폼 제출 완료 | 주선자 |
| `verification_done` | 지인 본인 인증 완료 | — |
| `edit_requested` | 주선자가 친구에게 폼 수정 요청 | 친구 |
| `acquaintance_blocked` | 지인이 차단됨 | — |

> 아래 타입은 폐지된 당사자 동의 흐름 잔재로 **현재 발송되지 않습니다**:
> `match_accepted`, `match_consent_requested`, `match_counterpart_consented`, `match_consent_rejected`, `match_consent_expired`
> (특히 수락 알림은 `match_accepted`가 아니라 양쪽에 `matched`로 발송됩니다.)

---

## 5. API 명세

---

### 5.1 인증 (Auth)

#### 토큰 갱신

```
POST /destiny/api/auth/refresh
```

**Headers**
```
X-Refresh-Token: {refreshToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": null
  }
}
```

---

#### 로그아웃

```
POST /destiny/api/auth/logout
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.2 사용자 (User)

#### 내 정보 조회

```
GET /destiny/api/users/me
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "nickname": "닉네임",
    "role": "USER",              // "USER" | "ADMIN"
    "phoneVerified": true,
    "maskedPhone": "010-****-5678"  // 미인증 시 null
  }
}
```

---

#### 닉네임 변경

```
PATCH /destiny/api/users/me/nickname
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{ "nickname": "새닉네임" }
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.3 주선자 (Registrant)

> 주선자 = 가입한 일반 사용자. 다른 주선자를 팔로우하고 그들의 지인과 매칭할 수 있습니다.

#### 주선자 목록 조회

```
GET /destiny/api/registrants
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "uuid",
      "nickname": "홍길동",
      "bio": "소개글",
      "publishedProfileCount": 3,
      "followerCount": 12,
      "isFollowing": false
    }
  ]
}
```

---

#### 특정 주선자 조회

```
GET /destiny/api/registrants/{userId}
Authorization: Bearer {accessToken}
```

**Response** `200` — RegistrantSummaryResponse (위와 동일 구조)

---

#### 주선자의 팔로잉 목록

```
GET /destiny/api/registrants/{userId}/following
Authorization: Bearer {accessToken}
```

**Response** `200` — `RegistrantSummaryResponse[]`

---

#### 주선자의 팔로워 목록

```
GET /destiny/api/registrants/{userId}/followers
Authorization: Bearer {accessToken}
```

**Response** `200` — `RegistrantSummaryResponse[]`

---

#### 내 소개글(bio) 수정

```
PATCH /destiny/api/registrants/me/bio
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{ "bio": "안녕하세요, 좋은 인연을 연결해드립니다." }
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.4 팔로우 (Follow)

#### 팔로우

```
POST /destiny/api/users/{userId}/follow
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 언팔로우

```
DELETE /destiny/api/users/{userId}/follow
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 팔로우 상태 조회

```
GET /destiny/api/users/{userId}/follow-status
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "following": true,
    "mutual": false,
    "followerCount": 10,
    "followingCount": 5
  }
}
```

---

### 5.5 지인 프로필 등록 — 폼 (Form)

> 주선자(마담)가 지인(매물)에게 폼 링크를 전달합니다.  
> 지인은 **카카오 로그인 후** 직접 자신의 정보를 입력하고 제출합니다.

#### 폼 플로우 요약

**[프론트] 폼 링크 진입**
```
친구가 /destiny/form/{madamId} 접속
  → GET /destiny/form/{madamId} 로 마담 존재 확인
  → 프론트: madamId를 localStorage에 저장
  → "카카오 로그인" 버튼 노출
```

**[프론트 → 서버] 카카오 로그인**
```
GET /destiny/oauth2/authorization/kakao 로 리다이렉트
  → 카카오 인증 완료
  → 서버가 /destiny/oauth2/callback?accessToken=...&refreshToken=...&userId=...&profileImageUrl=... 로 리다이렉트
  → 프론트: 토큰 저장, localStorage에서 madamId 복원
  → profileImageUrl 존재 시 "카카오 프로필 사진을 사용할까요?" 팝업 표시
```

**[프론트 → 서버] 프로필 제출**
```
친구가 폼 작성 완료
  → POST /destiny/form/{madamId}  (JWT 필요)
     body: { useKakaoPhoto, name, age, gender, job, intro, mbti, hobbies, phoneNumber, kakaoId, instagramId }
  → 응답: { acquaintanceId, uploadToken, status: "PENDING_APPROVAL" }
```

**[프론트 → 서버] 추가 사진 업로드 (선택)**
```
  → POST /destiny/form/{uploadToken}/photos  (JWT 필요)
     최대 1장 (카카오 사진 포함)
```

**[마담] 승인**
```
  → GET  /destiny/api/profiles/{acquaintanceId}       제출 내용 확인
  → POST /destiny/api/profiles/{acquaintanceId}/approve 또는 /reject
```

---

#### 폼 링크 유효성 확인

```
GET /destiny/form/{madamId}
```

> `{madamId}` = 주선자의 userId (영구 고유값, 만료 없음)

**Response** `200`
```json
{ "success": true, "message": "유효한 폼 링크입니다.", "data": null }
```

**Error**
- `404` 존재하지 않는 마담 ID

---

#### 프로필 제출

```
POST /destiny/form/{madamId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "useKakaoPhoto": true,
  "name": "김지인",
  "age": 27,
  "gender": "female",
  "job": "디자이너",
  "intro": "안녕하세요!",
  "mbti": "INFJ",
  "hobbies": "독서, 요가",
  "phoneNumber": "01012345678",
  "kakaoId": "kakao_id",
  "instagramId": "insta_id"
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `useKakaoPhoto` | Y | 카카오 프로필 사진을 첫 번째 사진으로 등록할지 여부 |
| `name` | Y | 최대 50자 |
| `age` | Y | 10~99 |
| `gender` | N | `"male"` \| `"female"` \| `"other"` |
| `phoneNumber` | Y | 최대 20자. **중복 체크 기준** |
| `mbti` | N | 대문자 4자리 (`^[A-Z]{4}$`) |
| `job`, `intro`, `hobbies`, `kakaoId`, `instagramId` | N | |

> **이메일**: 카카오 계정 이메일이 자동으로 저장됩니다. 별도 입력 불필요.

**등록 제한**
- 로그인한 사용자가 마담 본인인 경우(`madamId == 로그인 userId`) → `400 Bad Request`
- 같은 사용자 계정이 이미 다른 마담의 친구로 등록된 경우 → `409 Conflict`
- 같은 `phoneNumber`가 이미 **승인 완료(PUBLISHED)** 된 경우 → `409 Conflict` (전화번호 blind index 기준)
- **재제출(기존 작성분 수정)은 카드가 `DRAFT`(주선자의 수정 요청을 받음) 또는 `PENDING_APPROVAL`(아직 승인 전)일 때만 가능.** 그 외 상태(`PUBLISHED` 등)는 `409 Conflict` — 주선자에게 수정 요청을 받아야 함

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "acquaintanceId": "uuid",
    "uploadToken": "abc123...",
    "status": "PENDING_APPROVAL"
  }
}
```

| 필드 | 설명 |
|---|---|
| `acquaintanceId` | 생성된 프로필 ID (= `dating_profiles.id`) |
| `uploadToken` | 추가 사진 업로드용 토큰 (이 제출 건에만 유효) |
| `status` | 항상 `"PENDING_APPROVAL"` (마담 승인 대기) |

**Error**
- `400` 본인(마담)을 매물로 등록하려는 경우
- `401` 로그인 필요
- `404` 유효하지 않은 madamId
- `409` 이미 등록된 계정이거나, 이미 PUBLISHED 상태로 등록된 전화번호
- `409` `주선자에게 폼 수정 요청을 받은 카드만 수정할 수 있습니다.` — 재제출인데 카드가 `DRAFT`/`PENDING_APPROVAL`이 아님

---

#### 사진 업로드 (추가)

```
POST /destiny/form/{uploadToken}/photos
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

> `{uploadToken}` = 프로필 제출 응답의 `uploadToken` 값  
> 최대 1장 (카카오 사진 포함). 추후 여러 장으로 확장 예정.

**Form Data**
- `file`: 이미지 파일

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": "https://cdn.example.com/photos/xxx.jpg"
}
```

---

### 5.6 지인 관리 (Acquaintance)

> 주선자가 자신의 친구(지인)를 관리합니다. **모든 엔드포인트는 `/api/profiles` 하위입니다.**
>
> ⚠️ **2026-06-06 변경**: 이 영역의 엔드포인트는 `/api/acquaintances/**` → **`/api/profiles/**`로 이전**됐습니다. (구 경로 제거됨)

#### 내 친구 목록 조회

```
GET /destiny/api/profiles
Authorization: Bearer {accessToken}
```

> 내가 주선자로서 등록한 친구 목록. 최신 제출 순 정렬. (구 `GET /api/acquaintances`)

**Response** `200`
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

> `registrationStatus` 값은 `ProfileStatus` (`DRAFT` | `PENDING_APPROVAL` | `PUBLISHED` 등). `verifiedAt` = 승인(PUBLISHED) 시각.

---

#### 내 폼 링크 조회

```
GET /destiny/api/profiles/my-form
Authorization: Bearer {accessToken}
```

> 마담마다 영구 고유 폼 링크. 만료 없음. 매번 동일한 URL 반환.

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "formUrl": "https://app.example.com/form/{madamUserId}"
  }
}
```

---

#### 친구 상세 조회

> `GET /destiny/api/profiles/{id}` 사용 — 응답은 **`ProfileDetailResponse`** ([5.7 프로필 상세 조회](#프로필-상세-조회) 참조).
> (구 `GET /api/acquaintances/{id}`는 제거됨. 기존 `AcquaintanceDetailResponse` 형태 상세는 더 이상 제공되지 않으며, 목록 응답은 `AcquaintanceDetailResponse` 유지)

---

#### 친구 승인 (주선자가 폼 제출 확인 → PUBLISHED)

```
POST /destiny/api/profiles/{id}/approve
Authorization: Bearer {accessToken}
```

> `PENDING_APPROVAL` 상태에서만 가능. 승인 시 `PUBLISHED`가 되어 카드/매칭에 노출. (그 외 상태면 `409`)

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 친구 거절

```
POST /destiny/api/profiles/{id}/reject
Authorization: Bearer {accessToken}
```

> soft delete 처리되어 목록에서 사라집니다.

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 친구에게 수정 요청 (PENDING_APPROVAL·PUBLISHED → DRAFT)

```
POST /destiny/api/profiles/{id}/request-edit
Authorization: Bearer {accessToken}
```

> 승인 대기 또는 공개된 카드를 `DRAFT`로 되돌리고 친구에게 알림(`edit_requested`)을 보냅니다. 친구가 폼을 다시 제출하면 `PENDING_APPROVAL`로 복귀. 친구는 `DRAFT`(=수정 요청을 받은) 또는 `PENDING_APPROVAL`(아직 승인 전) 상태 카드만 재제출할 수 있습니다.
>
> **매칭 상태 게이트** — 수정 요청 전 해당 프로필의 매칭을 먼저 정리해야 합니다.

| HTTP | message | 조건 |
| --- | --- | --- |
| `409` | `이미 매칭이 진행 중이거나 성사된 프로필은 수정 요청할 수 없습니다.` | 수락/동의 진행 중 또는 `MATCHED` |
| `409` | `받은 매칭 요청을 모두 거절한 뒤 수정 요청이 가능합니다.` | 받은 `PENDING` 요청 존재 |
| `409` | `보낸 매칭 요청을 모두 취소한 뒤 수정 요청이 가능합니다.` | 보낸 `PENDING` 요청 존재 |
| `409` | `승인 대기 또는 공개 상태에서만 수정 요청할 수 있습니다.` | 카드가 `PENDING_APPROVAL`/`PUBLISHED`가 아님 |

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.7 데이팅 프로필 (Profile)

> 지인의 공개 소개팅 프로필. 주선자가 직접 생성하거나, 초대 링크를 통해 당사자가 승인합니다.

#### 프로필 생성

```
POST /destiny/api/profiles
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "name": "김소개",
  "age": 26,
  "gender": "female",           // "male" | "female" | "other" | null
  "isStudent": false,           // 필수
  "schoolName": "서울대학교",
  "major": "경영학",
  "occupation": "마케터",
  "mbti": "ENFP",
  "hobby": "영화감상, 러닝",
  "introduction": "안녕하세요, 활발한 성격입니다.",
  "subjectPhone": "01012345678", // 필수, 당사자 전화번호
  "kakaoId": "kakao_abc",
  "instagramId": "insta_abc"
}
```

**Validation**
- `name`: 필수, 최대 100자
- `age`: 필수, 18~99
- `isStudent`: 필수
- `subjectPhone`: 필수, `^0[0-9]{8,10}$`
- `introduction`: 최대 2000자

**Response** `201` — ProfileDetailResponse

---

#### 내 프로필(친구) 목록 조회

> `GET /destiny/api/profiles` — [5.6 내 친구 목록 조회](#내-친구-목록-조회) 참조.
> ⚠️ **2026-06-06 변경**: 응답이 기존 `ProfileSummaryResponse[]`(status/visibility/firstPhotoUrl/createdAt)에서 **`AcquaintanceDetailResponse[]`**(name/age/job/intro/registrationStatus/photoUrls 등)로 바뀌었습니다.

---

#### 공개 프로필 목록 조회 (다른 주선자의 프로필 탐색)

```
GET /destiny/api/profiles/public?registrantId={registrantId}&gender={gender}
Authorization: Bearer {accessToken}
```

**Query Parameters**
- `registrantId` (optional): 특정 주선자의 프로필만 조회
- `gender` (optional): 성별 필터 — `"male"` \| `"female"` \| `"other"`

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "uuid",
      "name": "김소개",
      "age": 26,
      "gender": "female",
      "isStudent": false,
      "schoolName": "서울대학교",
      "major": "경영학",
      "occupation": "마케터",
      "mbti": "ENFP",
      "hobby": "영화감상, 러닝",
      "introduction": "안녕하세요",
      "firstPhotoUrl": "https://cdn.example.com/photos/1.jpg"
    }
  ]
}
```

> 공개 조회 시 `kakaoId`, `instagramId`, `subjectPhone`은 포함되지 않습니다.

---

#### 프로필 상세 조회

```
GET /destiny/api/profiles/{id}
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "registrantId": "uuid",
    "registrantNickname": "주선자닉네임",
    "status": "PUBLISHED",
    "name": "김소개",
    "age": 26,
    "gender": "female",
    "isStudent": false,
    "schoolName": "서울대학교",
    "major": "경영학",
    "occupation": "마케터",
    "mbti": "ENFP",
    "hobby": "영화감상, 러닝",
    "introduction": "안녕하세요",
    "kakaoId": "kakao_abc",       // 등록자 본인만 조회 가능, 그 외 null
    "instagramId": "insta_abc",   // 등록자 본인만 조회 가능, 그 외 null
    "subjectPhone": "010****5678",// 등록자 본인만 조회 가능, 그 외 null
    "photoUrls": ["https://cdn.example.com/photos/1.jpg"],
    "createdAt": "2026-05-01T10:00:00",
    "updatedAt": "2026-05-10T10:00:00"
  }
}
```

---

#### 프로필 수정

```
PATCH /destiny/api/profiles/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body** (모든 필드 optional)
```json
{
  "name": "수정된이름",
  "age": 27,
  "gender": "female",
  "isStudent": true,
  "schoolName": "서울대학교",
  "major": "경영학",
  "occupation": "마케터",
  "mbti": "ENFP",
  "hobby": "독서",
  "introduction": "수정된 소개글",
  "kakaoId": "new_kakao",
  "instagramId": "new_insta"
}
```

**Response** `200` — ProfileDetailResponse

---

#### 프로필 삭제

```
DELETE /destiny/api/profiles/{id}
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 프로필 사진 업로드

```
POST /destiny/api/profiles/{id}/photos
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

**Form Data**
- `file`: 이미지 파일

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": "https://cdn.example.com/photos/xxx.jpg"
}
```

---

#### 프로필 사진 삭제

```
DELETE /destiny/api/profiles/{id}/photos/{photoId}
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 프로필 공개 범위 변경

```
PATCH /destiny/api/profiles/{id}/visibility?visibility={value}
Authorization: Bearer {accessToken}
```

**Query Parameters**
- `visibility`: `PUBLIC` | `FOLLOWERS_ONLY` | `PRIVATE`

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 프로필 신고

```
POST /destiny/api/profiles/{id}/reports
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{ "reason": "신고 사유" }
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.8 초대 링크 — 당사자 승인 흐름 (Invitation)

> 주선자(A)가 프로필 초대 링크를 생성 → 당사자(B)가 링크로 접근해 승인/거절하는 흐름

#### 초대 링크 생성 (주선자 A)

```
POST /destiny/api/profiles/{id}/invite
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "inviteUrl": "https://app.example.com/invitations/abc123token",
    "expiresAt": "2026-06-06T12:00:00"
  }
}
```

---

#### 초대 링크 정보 조회 (당사자 B — 인증 불필요)

```
GET /destiny/api/invitations/{token}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "profileId": "uuid",
    "registrantNickname": "홍주선",
    "subjectName": "김소개",
    "status": "INVITED"
  }
}
```

---

#### 당사자 본인 프로필 조회 (로그인 + 전화번호 인증 후)

```
GET /destiny/api/invitations/{token}/profile
Authorization: Bearer {accessToken}
```

**Response** `200` — ProfileDetailResponse

---

#### 당사자 프로필 수정

```
PATCH /destiny/api/invitations/{token}/profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body** — ProfileUpdateRequest (모든 필드 optional)

**Response** `200` — ProfileDetailResponse

---

#### 당사자 개인정보 동의

```
POST /destiny/api/invitations/{token}/consent
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 당사자 승인

```
POST /destiny/api/invitations/{token}/approve
Authorization: Bearer {accessToken}
```

**Response** `200` — ProfileDetailResponse

---

#### 당사자 거절

```
POST /destiny/api/invitations/{token}/reject
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body** (optional)
```json
{ "reason": "거절 사유" }
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.9 카드 (Card)

> **다른 마담의 매물을 탐색하는 API.** `VERIFIED` + `PUBLIC` 상태인 Acquaintance만 노출됩니다.  
> 본인이 등록한 매물, 차단한 매물은 결과에서 제외됩니다. 순서는 랜덤입니다.

#### 카드 목록 조회

```
GET /destiny/api/cards
Authorization: Bearer {accessToken}
```

**필터 조건**
- `ProfileStatus = PUBLISHED` (마담 승인 완료)
- `Visibility = PUBLIC`
- 본인(`userId`) 소유 매물 제외
- 차단한 매물 제외
- 정렬: 랜덤

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "uuid",
      "name": "김소개",
      "age": 26,
      "gender": "female",
      "mbti": "ENFP",
      "thumbnail": "https://cdn.example.com/photos/thumb.jpg"
    }
  ]
}
```

---

#### 카드 상세 조회

```
GET /destiny/api/cards/{id}
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "name": "김소개",
    "age": 26,
    "gender": "female",
    "job": "디자이너",
    "intro": "안녕하세요!",
    "mbti": "ENFP",
    "hobbies": "독서, 요가",
    "photoUrls": ["https://cdn.example.com/photos/1.jpg"]
  }
}
```

**Error**
- `404` 존재하지 않거나 조회 불가한 카드 (VERIFIED+PUBLIC 아닌 경우 포함)

---

### 5.10 매칭 (Matching)

> 주선자 A가 자신의 지인 B와 다른 주선자 C의 지인 D를 연결하는 요청
>
> 📄 보내기·받기 화면 구현은 **[matching-frontend-guide.md](./matching-frontend-guide.md)** 참고 (에러 케이스·타이밍·체크리스트 포함).
> 수신자 수락 시 **별도 당사자 동의 없이 즉시 `MATCHED`** 가 됩니다.

#### 매칭 요청 생성

```
POST /destiny/api/matchings
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "requesterProfileId": "uuid",   // 내 지인 B의 DatingProfile ID (필수)
  "targetProfileId": "uuid",       // 상대 지인 D의 DatingProfile ID (필수)
  "message": "잘 어울릴 것 같아서 연결해드립니다."  // 최대 200자, optional
}
```

**Response** `200` — MatchingResponse

---

#### 보낸 매칭 목록

```
GET /destiny/api/matchings/sent
Authorization: Bearer {accessToken}
```

**Response** `200` — `MatchingResponse[]`

---

#### 받은 매칭 목록

```
GET /destiny/api/matchings/received
Authorization: Bearer {accessToken}
```

**Response** `200` — `MatchingResponse[]`

---

#### 성사된 매칭 목록

```
GET /destiny/api/matchings/matched
Authorization: Bearer {accessToken}
```

**Response** `200` — `MatchingResponse[]`

---

#### 매칭 상세 조회

```
GET /destiny/api/matchings/{id}
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "requesterProfile": {
      "id": "uuid",
      "name": "김소개",
      "gender": "FEMALE"
    },
    "targetProfile": {
      "id": "uuid",
      "name": "이소개",
      "gender": "MALE"
    },
    "requesterNickname": "주선자A닉네임",
    "receiverNickname": "주선자C닉네임",
    "status": "PENDING",
    "message": "잘 어울릴 것 같아서요",
    "rejectReason": null,
    "createdAt": "2026-05-30T10:00:00",
    "receiverRespondedAt": null,
    "receiverExpiresAt": "2026-06-02T10:00:00"
  }
}
```

---

#### 매칭 수락 (수신자 C)

```
POST /destiny/api/matchings/{id}/accept
Authorization: Bearer {accessToken}
```

> 수락 즉시 `status: "MATCHED"` 로 성사됩니다. 응답 기한(72h) 초과 후 수락 시 `410 Gone`.

**Response** `200` — MatchingResponse (`status: "MATCHED"`)

---

#### 매칭 거절 (수신자 C)

```
POST /destiny/api/matchings/{id}/reject
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body** (optional)
```json
{ "reason": "거절 사유" }
```

**Response** `200` — MatchingResponse

---

#### 매칭 취소 (요청자 A, PENDING 상태에서만 가능)

```
POST /destiny/api/matchings/{id}/cancel
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 연락처 조회 (매칭 성사 후)

```
GET /destiny/api/matchings/{id}/contact
Authorization: Bearer {accessToken}
```

> 매칭 status가 `MATCHED` 일 때만 응답합니다.

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "name": "이소개",
    "kakaoId": "kakao_target",
    "instagramId": "insta_target"
  }
}
```

---

### ~~5.11 당사자 동의 (Candidate Consent)~~ — 폐지

> ⚠️ **폐지됨**: 매칭 수락 시 당사자(B, D)가 별도로 최종 동의하던 2단계 흐름은 제거되었습니다.
> 이제 수신자(C)가 `POST /api/matchings/{id}/accept` 하면 **즉시 `MATCHED`** 가 됩니다.
>
> `/api/candidate-consents/**` 엔드포인트는 코드상 남아 있으나 진입 경로가 없어 **호출하지 마세요** (대기 목록은 항상 비어 있음). 프론트는 구현하지 않습니다.

---

### 5.12 전화번호 인증 (Phone Verification)

> 초대 링크(Invitation) 흐름에서 당사자(B)가 본인 확인을 위해 사용합니다.  
> OTP는 기본적으로 SMS로 발송되며, 서버 설정(`SMS_PROVIDER=email`)에 따라 이메일로도 수신할 수 있습니다.

#### OTP 발송

```
POST /destiny/api/phone-verifications/send
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "phone": "01012345678",
  "invitationToken": "abc123token",
  "email": "user@example.com"
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `phone` | Y | 당사자 전화번호 (`^0[0-9]{8,10}$`) |
| `invitationToken` | Y | 초대 링크 토큰 |
| `email` | N | 이메일로 OTP 수신할 경우 입력 (없으면 SMS 발송) |

> **이메일 OTP 흐름**: `email` 필드를 포함하면 SMS 대신 해당 이메일 주소로 OTP 발송.  
> 서버 환경변수 `SMS_PROVIDER=email` 설정과 무관하게, 클라이언트에서 `email` 값 유무로 발송 채널 결정.

**Response** `200`
```json
{ "success": true, "message": "OTP가 발송됐습니다.", "data": null }
```

**제약**
- OTP 유효시간: **5분**
- 최대 시도 횟수: **3회** (초과 시 `429 Too Many Requests`)

---

#### OTP 인증

```
POST /destiny/api/phone-verifications/verify
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "phone": "01012345678",
  "otp": "123456",
  "invitationToken": "abc123token"
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `phone` | Y | 전화번호 — 주선자가 프로필에 등록한 번호와 일치해야 함 |
| `otp` | Y | 6자리 인증번호 |
| `invitationToken` | Y | 초대 링크 토큰 |

**인증 성공 시 처리**
- 로그인 계정에 전화번호 저장
- 동일인 감지: 로그인 계정이 주선자(A)와 동일하거나, 전화번호가 A의 번호와 일치하면 `REVIEW_REQUIRED`(관리자 검수 대기)로 전이
- 그 외: `PENDING_APPROVAL`(주선자 검수 대기)로 전이

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "profileId": "uuid",
    "newStatus": "PENDING_APPROVAL"
  }
}
```

| `newStatus` 값 | 의미 |
|---|---|
| `PENDING_APPROVAL` | 일반 케이스 — 주선자(A) 검수 대기 |
| `REVIEW_REQUIRED` | 동일인 감지 — 관리자 검수 대기 |

---

### 5.13 알림 (Notification)

#### 읽지 않은 알림 목록

```
GET /destiny/api/notifications
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "uuid",
      "type": "match_request",
      "matchingId": "uuid",
      "consentId": "uuid",
      "isRead": false,
      "createdAt": "2026-05-30T10:00:00"
    }
  ]
}
```

---

#### 알림 읽음 처리

```
PATCH /destiny/api/notifications/{id}/read
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.14 차단 (Block)

#### 지인 차단

```
POST /destiny/api/blocks
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{ "acquaintanceId": "uuid" }
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 차단 해제

```
DELETE /destiny/api/blocks/{acquaintanceId}
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

### 5.15 관리자 (Admin)

> `ADMIN` 역할 전용. 일반 사용자 접근 시 `403 Forbidden`.

#### 프로필 목록 조회 (상태 필터)

```
GET /destiny/api/admin/profiles?status={ProfileStatus}
Authorization: Bearer {accessToken}
```

**Response** `200` — `AdminProfileResponse[]`

---

#### 프로필 숨김 처리

```
PATCH /destiny/api/admin/profiles/{id}/suspend
Authorization: Bearer {accessToken}
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

#### 프로필 검수 (승인/거절)

```
PATCH /destiny/api/admin/profiles/{id}/review
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "approved": true,
  "reason": "거절 사유 (approved=false 일 때)"
}
```

**Response** `200` — ProfileDetailResponse

---

#### 신고 목록 조회

```
GET /destiny/api/admin/reports?status={status}
Authorization: Bearer {accessToken}
```

**Response** `200` — `AdminReportResponse[]`

---

#### 신고 상태 업데이트

```
PATCH /destiny/api/admin/reports/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "status": "PROCESSED",
  "memo": "처리 메모"
}
```

**Response** `200` — AdminReportResponse

---

#### 매칭 현황 조회

```
GET /destiny/api/admin/matchings?status={MatchingStatus}
Authorization: Bearer {accessToken}
```

**Response** `200` — `AdminMatchingResponse[]`

---

## 부록: 주요 흐름 요약

### A. 지인 등록 (폼 방식)

```
1. GET  /destiny/api/profiles/my-form                   → 영구 폼 URL 획득 (마담)
2. 지인이 GET  /destiny/form/{madamId}                  → 링크 유효성 확인 (public)
3. 지인이 카카오 로그인 (/destiny/oauth2/authorization/kakao)
   → 콜백: profileImageUrl 있으면 "카카오 사진 사용?" 팝업 표시
4. 지인이 POST /destiny/form/{madamId}                  → 프로필 제출 (JWT 필요)
   → { acquaintanceId, uploadToken, status: "PENDING_APPROVAL" }
5. 지인이 POST /destiny/form/{uploadToken}/photos        → 추가 사진 업로드 (optional, 최대 1장)
6. 마담이 GET  /destiny/api/profiles/{id}               → 제출 내용 확인
7. 마담이 POST /destiny/api/profiles/{id}/approve       → 최종 승인 (PUBLISHED)
```

### B. 당사자 승인 흐름 (초대 링크)

```
1. POST /destiny/api/profiles/{id}/invite                    → inviteUrl 획득
2. 당사자가 GET /destiny/api/invitations/{token}             → 프로필 정보 확인 (public)
3. 당사자 로그인 (OAuth2)
4. POST /destiny/api/phone-verifications/send                → OTP 발송
5. POST /destiny/api/phone-verifications/verify              → 본인 인증
6. GET /destiny/api/invitations/{token}/profile              → 내 프로필 확인
7. PATCH /destiny/api/invitations/{token}/profile            → 내용 수정 (optional)
8. POST /destiny/api/invitations/{token}/consent             → 개인정보 동의
9. POST /destiny/api/invitations/{token}/approve             → 최종 승인
```

### C. 매칭 요청 ~ 성사

```
1. POST /destiny/api/matchings                               → 매칭 요청 (A → C), status: PENDING
2. POST /destiny/api/matchings/{id}/accept                   → C가 수락 → 즉시 status: MATCHED
3. GET /destiny/api/matchings/{id}/contact                   → 매칭 성사 후 상대 친구 연락처 조회
```
