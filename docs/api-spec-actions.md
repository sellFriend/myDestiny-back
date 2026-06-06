# My Destiny — 주요 액션 API 명세

> Base URL: `http://localhost:8888/destiny`  
> 모든 요청에 `Authorization: Bearer {accessToken}` 헤더 필요

---

## 1. 헤더 알림

### 읽지 않은 알림 목록 조회

```
GET /api/notifications
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

> `form_submitted` 타입은 `matchingId` 필드에 `acquaintanceId` 값이 담깁니다. (서버 내부적으로 같은 컬럼 사용)

| `type` 값 | 설명 | 응답 포함 |
|---|---|---|
| `form_submitted` | 지인이 폼 제출 완료 (마담 수신) | O |
| `match_request` | 매칭 요청 받음 | O |
| `match_accepted` | 매칭 수락됨 | O |
| `match_rejected` | 매칭 거절됨 | O |
| `matched` | 매칭 성사 | O |

### 알림 읽음 처리

```
PATCH /api/notifications/{id}/read
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

---

## 2. 폼 승인 / 거절 (마담)

> 지인이 폼 제출 완료(`PENDING_APPROVAL`) 후 마담이 확인하여 승인 또는 거절
> ⚠️ **2026-06-06 변경**: 경로가 `/api/acquaintances/**` → `/api/profiles/**`로 이전됨.

### 승인

```
POST /api/profiles/{id}/approve
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

- 승인 시 `ProfileStatus` → `PUBLISHED`

**Error**
- `403` 본인 소유 지인이 아닌 경우
- `404` 지인을 찾을 수 없는 경우
- `409` `PENDING_APPROVAL` 상태가 아닌 경우

### 거절

```
POST /api/profiles/{id}/reject
```

**Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

- 거절 시 소프트 딜리트 처리 — 마담의 지인 관리 목록 및 카드 탐색에서 제거

**Error**
- `403` 본인 소유 지인이 아닌 경우
- `404` 지인을 찾을 수 없는 경우

---

## 3. 매칭 요청 수락 / 거절 (수신자 마담)

> 매칭 요청을 받은 주선자(수신자)가 수락 또는 거절

### 수락

```
POST /api/matchings/{id}/accept
```

**Response** `200` — MatchingResponse

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "status": "MATCHED",
    "requesterProfile": { "id": "uuid", "name": "김소개", "gender": "FEMALE" },
    "targetProfile": { "id": "uuid", "name": "이소개", "gender": "MALE" },
    "requesterNickname": "주선자A",
    "receiverNickname": "주선자C",
    "message": "잘 어울릴 것 같아서요",
    "rejectReason": null,
    "createdAt": "2026-05-30T10:00:00",
    "receiverRespondedAt": "2026-05-30T11:00:00",
    "receiverExpiresAt": "2026-06-06T10:00:00"
  }
}
```

- 수락 즉시 매칭 성사 (`MATCHED`), 요청자·수신자 양쪽에 `matched` 알림 발송

**Error**
- `403` 수신자가 아닌 경우
- `404` 매칭을 찾을 수 없는 경우
- `409` 이미 처리된 매칭
- `410` 응답 기한 만료

### 거절

```
POST /api/matchings/{id}/reject
Content-Type: application/json
```

**Request Body** (optional)
```json
{ "reason": "거절 사유" }
```

**Response** `200`

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "status": "REJECTED_BY_RECEIVER",
    "requesterProfile": { "id": "uuid", "name": "김소개", "gender": "FEMALE" },
    "targetProfile": { "id": "uuid", "name": "이소개", "gender": "MALE" },
    "requesterNickname": "주선자A",
    "receiverNickname": "주선자C",
    "message": "잘 어울릴 것 같아서요",
    "rejectReason": "거절 사유",
    "createdAt": "2026-05-30T10:00:00",
    "receiverRespondedAt": "2026-05-30T11:00:00",
    "receiverExpiresAt": "2026-06-06T10:00:00"
  }
}
```

**Error**
- `403` 수신자가 아닌 경우
- `404` 매칭을 찾을 수 없는 경우
- `409` 이미 처리된 매칭
