# 매칭 요청(보내기·받기) — 프론트 구현 가이드

매칭 요청을 **보내는 쪽(요청자 A)** 과 **받는 쪽(수신자 C)** 의 화면을 구현하기 위한 API 가이드입니다.
프로필 등록/공개 흐름은 `frontend-api-spec.md`를 참고하세요.

---

## 0. 공통 사항

- **Base URL**: `http://localhost:8888/destiny` (운영은 `https://fixlog.art/destiny`)
- 모든 엔드포인트 **JWT 필요**: `Authorization: Bearer {accessToken}`
- 성공 응답 형식:
  ```json
  { "success": true, "message": "OK", "data": { ... } }
  ```
- 에러 응답 형식 (HTTP status는 케이스별로 다름):
  ```json
  { "success": false, "message": "사람이 읽을 메시지", "data": null }
  ```

### 등장 인물

```
주선자 A (requester)            주선자 C (receiver)
   └ 내 친구 B (requesterProfile)   └ 상대 친구 D (targetProfile)
```

- **A**: 매칭을 *보내는* 로그인 사용자. 자기가 등록한 프로필 B를 들고 요청.
- **C**: 매칭을 *받는* 로그인 사용자. 자기가 등록한 프로필 D가 대상.
- 매칭은 **프로필(B↔D) 단위**로 맺어지고, 알림·수락/거절은 **주선자(A/C) 계정**이 수행합니다.

---

## 1. 전체 흐름

```
[A] POST /api/matchings              → status: PENDING   (C에게 MATCH_REQUEST 알림)
        ↓
[C] POST /api/matchings/{id}/accept  → status: MATCHED   (A·C 양쪽에 MATCHED 알림)
   또는
[C] POST /api/matchings/{id}/reject  → status: REJECTED_BY_RECEIVER (A에게 MATCH_REJECTED 알림)
   또는
[A] POST /api/matchings/{id}/cancel  → status: CANCELLED  (PENDING 상태에서만)
        ↓
매칭 성사(MATCHED) 후
[A 또는 C] GET /api/matchings/{id}/contact → 상대 친구 연락처(카카오/인스타) 공개
```

> 수신자가 `accept` 하면 **별도 당사자 동의 단계 없이 곧바로 `MATCHED`** 가 됩니다.
> 화면은 `PENDING` / `MATCHED` / `REJECTED_BY_RECEIVER` / `CANCELLED` / `EXPIRED` 5개 상태만 다루면 됩니다.

### 응답 기한

- 수신자(C)는 요청 생성 후 **72시간** 안에 응답해야 합니다 (`receiverExpiresAt`).
- 기한이 지난 PENDING 요청은 서버가 자동으로 `EXPIRED` 처리합니다. 기한 후 `accept` 시도 시 `410 Gone`.

---

## 2. 공통 응답 객체 — `MatchingResponse`

`POST /api/matchings`, `sent`, `received`, `matched`, `{id}`, `accept`, `reject` 가 모두 이 형태를 반환합니다.

```json
{
  "id": "matching-uuid",
  "requesterProfile": { "id": "profile-B-uuid", "name": "민수", "gender": "MALE" },
  "targetProfile":    { "id": "profile-D-uuid", "name": "지은", "gender": "FEMALE" },
  "requesterNickname": "A의 닉네임",
  "receiverNickname":  "C의 닉네임",
  "status": "PENDING",
  "message": "안녕하세요, 좋은 인연 이어졌으면 합니다.",
  "rejectReason": null,
  "createdAt": "2026-06-06T14:00:00",
  "receiverRespondedAt": null,
  "receiverExpiresAt": "2026-06-09T14:00:00"
}
```

| 필드 | 설명 |
|------|------|
| `requesterProfile` / `targetProfile` | 친구 프로필 요약 (`id`, `name`, `gender`). `gender`: `MALE` \| `FEMALE` |
| `requesterNickname` / `receiverNickname` | 주선자(A/C) 닉네임 |
| `status` | `MatchingStatus` (아래 표) |
| `rejectReason` | 거절 사유. 거절 전이거나 미입력이면 `null` |
| `receiverRespondedAt` | 수신자 응답 시각. 미응답이면 `null` |
| `receiverExpiresAt` | 수신자 응답 마감 시각 (생성 +72h) |

### 화면에서 다루는 status

| 값 | 의미 | 보낸 목록 표시 | 받은 목록 표시 |
|----|------|----------------|----------------|
| `PENDING` | 응답 대기 | "응답 대기 중" + 취소 버튼 | 수락/거절 버튼 |
| `MATCHED` | 성사 | "매칭 성사" + 연락처 보기 | "매칭 성사" + 연락처 보기 |
| `REJECTED_BY_RECEIVER` | 수신자 거절 | "거절됨" (+ `rejectReason`) | — |
| `CANCELLED` | 요청자 취소 | "취소함" | "요청 취소됨" |
| `EXPIRED` | 기한 초과 | "만료됨" | "만료됨" |

---

## 3. 매칭 보내기 (요청자 A)

### 3.1 매칭 요청 생성

```
POST /api/matchings
Content-Type: application/json
```

**Request Body**
```json
{
  "requesterProfileId": "profile-B-uuid",
  "targetProfileId": "profile-D-uuid",
  "message": "안녕하세요, 좋은 인연 이어졌으면 합니다."
}
```

| 필드 | 필수 | 제약 | 설명 |
|------|------|------|------|
| `requesterProfileId` | ✅ | — | **내가 등록한** 친구 B의 프로필 ID |
| `targetProfileId` | ✅ | — | 상대 친구 D의 프로필 ID (카드에서 얻음) |
| `message` | ❌ | 최대 200자 | 수신자에게 전달할 한마디 |

**Response** `200` — `MatchingResponse` (`status: "PENDING"`)

**에러 케이스**

| status | message | 처리 |
|--------|---------|------|
| `404` | `프로필을 찾을 수 없습니다.` | 내 프로필 ID 오류 |
| `404` | `대상 프로필을 찾을 수 없습니다.` | 대상 프로필 ID 오류 |
| `403` | `접근 권한이 없습니다.` | `requesterProfileId`가 내 프로필이 아님 |
| `403` | `정지된 계정입니다.` | 요청자/수신자 정지 |
| `400` | `공개된 프로필만 매칭 요청이 가능합니다.` | 내 프로필이 `PUBLISHED` 아님 |
| `400` | `매칭 요청이 불가한 프로필입니다.` | 대상 프로필이 `PUBLISHED` 아님 |
| `400` | `자신의 프로필에 매칭 요청을 보낼 수 없습니다.` | 대상이 내 프로필 |
| `409` | `이미 진행 중인 매칭 요청이 있습니다.` | 같은 B↔D 조합 활성 매칭 존재 |
| `409` | `내 친구 프로필이 이미 다른 매칭 진행 중입니다.` | B가 다른 매칭 진행 중 |
| `409` | `상대 친구 프로필이 이미 다른 매칭 진행 중입니다.` | D가 다른 매칭 진행 중 |
| `409` | `최근 30일 내 거절된 조합입니다. 30일 후 재요청 가능합니다.` | 거절 후 30일 쿨다운 |

> 프로필은 **활성 매칭을 동시에 1개만** 가질 수 있습니다. 한 번 거절(수신자/당사자)되면 같은 조합은 **30일** 동안 재요청 불가.
> `409` 계열은 토스트로 `message`를 그대로 노출하면 됩니다.

### 3.2 보낸 요청 목록

```
GET /api/matchings/sent
```
**Response** `200` — `MatchingResponse[]` (최신순)

### 3.3 요청 취소 (PENDING 상태에서만)

```
POST /api/matchings/{id}/cancel
```
**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

| status | message | 처리 |
|--------|---------|------|
| `404` | `매칭을 찾을 수 없습니다.` | — |
| `403` | `접근 권한이 없습니다.` | 내가 요청자가 아님 |

---

## 4. 매칭 받기 (수신자 C)

### 4.1 받은 요청 목록

```
GET /api/matchings/received
```
**Response** `200` — `MatchingResponse[]` (최신순). `status: "PENDING"` 항목에 수락/거절 버튼 노출.

### 4.2 수락 → 즉시 성사

```
POST /api/matchings/{id}/accept
```
**Response** `200` — `MatchingResponse` (`status: "MATCHED"`). A·C 양쪽에 `MATCHED` 알림 발송.

| status | message | 처리 |
|--------|---------|------|
| `404` | `매칭을 찾을 수 없습니다.` | — |
| `403` | `접근 권한이 없습니다.` | 내가 수신자가 아님 |
| `409` | `이미 처리된 매칭입니다.` | PENDING이 아님 (목록 새로고침) |
| `410` | `응답 기한이 만료된 요청입니다.` | 72h 초과 — `EXPIRED`로 안내 |

### 4.3 거절

```
POST /api/matchings/{id}/reject
Content-Type: application/json
```
**Request Body** (선택, 생략 가능)
```json
{ "reason": "조건이 맞지 않아요" }
```
| 필드 | 필수 | 제약 |
|------|------|------|
| `reason` | ❌ | 최대 200자 |

**Response** `200` — `MatchingResponse` (`status: "REJECTED_BY_RECEIVER"`). A에게 `MATCH_REJECTED` 알림.

| status | message | 처리 |
|--------|---------|------|
| `404` | `매칭을 찾을 수 없습니다.` | — |
| `403` | `접근 권한이 없습니다.` | 내가 수신자가 아님 |
| `409` | `이미 처리된 매칭입니다.` | PENDING이 아님 |

---

## 5. 성사된 매칭 & 연락처

### 5.1 성사 매칭 목록

```
GET /api/matchings/matched
```
**Response** `200` — `MatchingResponse[]` (내가 요청자든 수신자든 `MATCHED`인 모든 건)

### 5.2 매칭 상세

```
GET /api/matchings/{id}
```
**Response** `200` — `MatchingResponse`. 요청자/수신자 당사자만 조회 가능(아니면 `403 접근 권한이 없습니다.`).

### 5.3 연락처 조회 (성사 후)

```
GET /api/matchings/{id}/contact
```
**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": { "name": "지은", "kakaoId": "jieun_k", "instagramId": "jieun.gram" }
}
```

- **상대편 친구의 연락처**가 반환됩니다. 요청자 측(A·B)이 보면 D의 연락처, 수신자 측(C·D)이 보면 B의 연락처.
- `kakaoId` / `instagramId`는 미입력 시 `null`일 수 있습니다.

| status | message | 처리 |
|--------|---------|------|
| `404` | `매칭을 찾을 수 없습니다.` | — |
| `403` | `성사된 매칭만 연락처를 조회할 수 있습니다.` | `MATCHED` 아님 |
| `403` | `접근 권한이 없습니다.` | 당사자(A/B/C/D)가 아님 |

---

## 6. 알림 연동

매칭 이벤트는 알림으로도 통지됩니다 (`GET /api/notifications`, 스펙 5.x 참고).

| 시점 | 받는 사람 | NotificationType |
|------|-----------|------------------|
| A가 요청 생성 | 수신자 C | `MATCH_REQUEST` |
| C가 수락 → 성사 | A·C 양쪽 | `MATCHED` |
| C가 거절 | 요청자 A | `MATCH_REJECTED` |

알림의 연관 ID(`referenceId`)는 `matchingId`이므로, 탭 시 `GET /api/matchings/{id}` 상세로 이동하면 됩니다.

---

## 7. 구현 체크리스트

- [ ] 카드에서 "매칭 요청" → 내 공개 프로필(B) 선택 → `POST /api/matchings`
- [ ] `409`/`400` 에러는 `message` 토스트로 노출 (특히 쿨다운·중복·미공개)
- [ ] 보낸 목록: `PENDING`에 취소 버튼, `REJECTED_BY_RECEIVER`에 `rejectReason` 노출
- [ ] 받은 목록: `PENDING`에 수락/거절, `receiverExpiresAt` 카운트다운 표시
- [ ] 수락 시 `410`이면 "만료된 요청" 안내 후 목록 새로고침
- [ ] `MATCHED` 항목에서만 "연락처 보기" 활성화 → `GET /api/matchings/{id}/contact`
