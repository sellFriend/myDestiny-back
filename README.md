# 내 운명 (My Destiny)

자신의 친구를 매물로 등록하고, 타인의 친구(매물)와 매칭시켜주는 소개팅 주선 서비스.

## 서비스 플로우

1. 마담(사용자)이 친구에게 초대 링크를 발송
2. 친구가 비로그인 상태로 자기소개 폼을 작성·제출
3. 마담이 폼을 검토 후 승인 → 매물이 카드로 노출됨
4. 다른 마담이 카드를 보고 매칭 요청 전송
5. 매물 소유 마담이 수락 → 양측 연락처가 공개됨

## 기술 스택

- Spring Boot 4.0.6 / Java 21 / MariaDB
- Spring Security (Google OAuth2 + JWT)
- Oracle Cloud Object Storage (S3 호환)
- Polling 기반 인앱 알림

---

## API 명세

### 공통

모든 응답은 아래 형식을 따른다.

```
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

`/api/**` 경로는 `Authorization: Bearer <accessToken>` 헤더 필요.  
`/form/**` 경로는 인증 불필요 (비로그인 친구 폼).

---

### Auth (인증)

#### Google OAuth2 로그인

```
GET /oauth2/authorization/google
```

브라우저를 Google 로그인 페이지로 리다이렉트. 인증 성공 시 프론트엔드로 리다이렉트:

```
{FRONTEND_URL}/oauth2/callback?accessToken=xxx&refreshToken=yyy
```

---

#### 액세스 토큰 갱신

```
POST /api/auth/refresh
```

| 헤더 | 필수 | 설명 |
|------|------|------|
| X-Refresh-Token | Y | 리프레시 토큰 |

**Response**
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": null
  }
}
```

---

#### 로그아웃

```
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

**Response** `data: null`

---

### Acquaintances (매물/친구 관리)

#### 초대 링크 생성

```
POST /api/acquaintances/invite
Authorization: Bearer <accessToken>
```

**Response**
```json
{
  "data": {
    "inviteUrl": "http://localhost:3000/form/abc123...",
    "expiresAt": "2026-06-05T00:00:00"
  }
}
```

> 링크 유효 기간: 7일

---

#### 내 매물 상세 조회

```
GET /api/acquaintances/{id}
Authorization: Bearer <accessToken>
```

**Response**
```json
{
  "data": {
    "id": "uuid",
    "name": "홍길동",
    "age": 25,
    "gender": "male",
    "job": "개발자",
    "intro": "안녕하세요",
    "mbti": "INFP",
    "hobbies": "독서, 등산",
    "registrationStatus": "verification_pending",
    "photos": ["https://..."],
    "createdAt": "2026-05-27T00:00:00"
  }
}
```

---

#### 매물 승인

```
POST /api/acquaintances/{id}/approve
Authorization: Bearer <accessToken>
```

승인 시 `registrationStatus`가 `verified`로 변경되며 카드 풀에 노출됨.  
매물 소유 마담에게 `verification_done` 알림 발송.

**Response** `data: null`

---

#### 매물 거절

```
POST /api/acquaintances/{id}/reject
Authorization: Bearer <accessToken>
```

`registrationStatus`가 `draft`로 변경. 친구에게 재제출 요청 가능.

**Response** `data: null`

---

### Form (비로그인 친구 폼)

#### 초대 링크 유효성 검증

```
GET /form/{token}
```

| 오류 | HTTP |
|------|------|
| 존재하지 않는 토큰 | 404 |
| 만료된 토큰 | 410 |

**Response** `data: null`, `message: "유효한 초대 링크입니다."`

---

#### 자기소개 폼 제출

```
POST /form/{token}
Content-Type: application/json
```

**Request Body**
```json
{
  "name": "홍길동",
  "age": 25,
  "gender": "male",
  "job": "개발자",
  "intro": "안녕하세요",
  "mbti": "INFP",
  "hobbies": "독서, 등산",
  "phoneNumber": "010-1234-5678",
  "email": "hong@example.com",
  "kakaoId": "hong123",
  "instagramId": "hong_insta"
}
```

| 필드 | 필수 | 제약 |
|------|------|------|
| name | Y | 최대 50자 |
| age | Y | 10~99 |
| gender | N | `male` / `female` |
| job | N | 최대 100자 |
| mbti | N | 대문자 4자 (예: INFP) |
| phoneNumber | Y | 최대 20자, 유니크 |
| email | Y | 이메일 형식 |
| kakaoId | N | 최대 100자 |
| instagramId | N | 최대 100자 |

**Response**
```json
{
  "data": {
    "acquaintanceId": "uuid",
    "status": "verification_pending"
  }
}
```

---

#### 사진 업로드

```
POST /form/{token}/photos
Content-Type: multipart/form-data
```

| 파라미터 | 설명 |
|----------|------|
| file | 이미지 파일 (최대 10MB, 최대 5장) |

**Response**
```json
{
  "data": "https://objectstorage.../acquaintances/uuid/xxx.jpg"
}
```

---

### Cards (카드 스와이프)

#### 카드 목록 조회

```
GET /api/cards
Authorization: Bearer <accessToken>
```

`verified` + `public` 상태의 매물을 랜덤으로 반환. 내 매물 및 차단한 매물 제외.

**Response**
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "홍길동",
      "age": 25,
      "gender": "male",
      "job": "개발자",
      "mbti": "INFP",
      "thumbnailUrl": "https://..."
    }
  ]
}
```

---

#### 카드 상세 조회

```
GET /api/cards/{id}
Authorization: Bearer <accessToken>
```

**Response**
```json
{
  "data": {
    "id": "uuid",
    "name": "홍길동",
    "age": 25,
    "gender": "male",
    "job": "개발자",
    "intro": "안녕하세요",
    "mbti": "INFP",
    "hobbies": "독서, 등산",
    "photos": ["https://..."]
  }
}
```

---

### Matchings (매칭)

#### 매칭 요청

```
POST /api/matchings
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body**
```json
{
  "myAcquaintanceId": "uuid",
  "targetAcquaintanceId": "uuid",
  "message": "안녕하세요, 소개 부탁드립니다!"
}
```

| 제약 | 내용 |
|------|------|
| 일일 한도 | 하루 5건 |
| 재요청 제한 | 거절 후 30일 이내 재요청 불가 |

수신자(매물 소유 마담)에게 `match_request` 알림 발송.

**Response**
```json
{
  "data": {
    "id": "uuid",
    "requesterAcquaintanceId": "uuid",
    "targetAcquaintanceId": "uuid",
    "status": "pending",
    "message": "안녕하세요...",
    "requestedAt": "2026-05-27T00:00:00",
    "respondedAt": null
  }
}
```

---

#### 받은 매칭 요청 목록

```
GET /api/matchings/received
Authorization: Bearer <accessToken>
```

**Response** `data: MatchingResponse[]` (requestedAt 내림차순)

---

#### 매칭 수락

```
POST /api/matchings/{id}/accept
Authorization: Bearer <accessToken>
```

`status`가 `accepted`로 변경. 요청자에게 `match_accepted` 알림 발송.

**Response** `data: MatchingResponse`

---

#### 매칭 거절

```
POST /api/matchings/{id}/reject
Authorization: Bearer <accessToken>
```

`status`가 `rejected`로 변경. 요청자에게 `match_rejected` 알림 발송.

**Response** `data: MatchingResponse`

---

#### 연락처 조회

```
GET /api/matchings/{id}/contact
Authorization: Bearer <accessToken>
```

수락(`accepted`)된 매칭의 요청자만 접근 가능.

**Response**
```json
{
  "data": {
    "name": "홍길동",
    "phoneNumber": "010-1234-5678",
    "kakaoId": "hong123",
    "instagramId": "hong_insta",
    "email": "hong@example.com"
  }
}
```

---

### Notifications (알림)

#### 읽지 않은 알림 목록

```
GET /api/notifications
Authorization: Bearer <accessToken>
```

**Response**
```json
{
  "data": [
    {
      "id": "uuid",
      "type": "match_request",
      "matchingId": "uuid",
      "isRead": false,
      "createdAt": "2026-05-27T00:00:00"
    }
  ]
}
```

| type | 발생 시점 |
|------|-----------|
| `match_request` | 매칭 요청 수신 |
| `match_accepted` | 내 매칭 요청이 수락됨 |
| `match_rejected` | 내 매칭 요청이 거절됨 |
| `verification_done` | 내 매물이 마담에게 승인됨 |
| `acquaintance_blocked` | 매물 차단 완료 |

---

#### 알림 읽음 처리

```
PATCH /api/notifications/{id}/read
Authorization: Bearer <accessToken>
```

**Response** `data: null`

---

### Blocks (차단)

#### 매물 차단

```
POST /api/blocks
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body**
```json
{
  "acquaintanceId": "uuid"
}
```

차단된 매물은 카드 목록에서 제외됨.

**Response** `data: null`

---

#### 차단 해제

```
DELETE /api/blocks/{acquaintanceId}
Authorization: Bearer <accessToken>
```

**Response** `data: null`

---

## 오류 코드

| HTTP | 상황 |
|------|------|
| 400 | 요청 값 오류 (유효성 검사 실패) |
| 401 | 인증 토큰 없음 또는 만료 |
| 403 | 권한 없음 (타인의 리소스 접근) |
| 404 | 리소스 없음 |
| 409 | 중복 또는 이미 처리된 요청 |
| 410 | 만료된 초대 링크 |
| 429 | 일일 매칭 요청 한도 초과 |
| 500 | 서버 내부 오류 |