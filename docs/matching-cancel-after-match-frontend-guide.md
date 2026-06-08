# 매칭 성사 후 취소 기능 — 프론트 대응 가이드

## 기능 요약

쌍방 매칭이 **성사(`MATCHED`)된 이후에도** 당사자가 매칭을 취소(해제)할 수 있게 되었습니다.

- 취소 가능 주체: **요청자·수신자 양쪽 모두** (성사된 매칭의 당사자라면 누구나)
- 취소 시 매칭 상태가 새 상태값 **`CANCELLED_AFTER_MATCH`** 로 전이
- 취소한 본인을 제외한 **상대방에게만** 새 알림 `match_released` 발송
- 취소 즉시 두 프로필의 점유가 해제되어 **바로 재매칭 가능** (현재 쿨다운 없음)

> ⚠️ 새 컬럼/마이그레이션 없음. `status`·알림 `type`은 모두 문자열로 저장됩니다.

---

## 1. 새 엔드포인트 — 성사된 매칭 취소

```
POST /api/matchings/{id}/cancel-match
```

| 항목 | 내용 |
|---|---|
| 인증 | 필요 (요청자 또는 수신자 본인) |
| 경로 변수 | `id` — 매칭 ID |
| 요청 바디 | 선택. `{ "reason": "사유" }` (생략 가능, 최대 200자) |
| 응답 | 취소된 매칭 객체 (`MatchingResponse`) |

### 요청 예시
```json
POST /api/matchings/{id}/cancel-match
{
  "reason": "사정이 생겨 취소합니다"
}
```

### 응답 예시
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "매칭 ID",
    "status": "CANCELLED_AFTER_MATCH",
    "...": "그 외 필드는 기존 MatchingResponse와 동일"
  }
}
```

### 에러

| 상황 | HTTP | 메시지 |
|---|---|---|
| 매칭 없음 | `404` | `매칭을 찾을 수 없습니다.` |
| 당사자가 아님 | `403` | `접근 권한이 없습니다.` |
| `MATCHED`가 아닌 상태에서 호출 | `409` | `성사된 매칭만 취소할 수 있습니다.` |

> 기존 `POST /api/matchings/{id}/cancel`(요청자가 **PENDING** 요청 취소)과는 **다른 엔드포인트**입니다.
> 진행 전(PENDING) 취소는 기존 `/cancel`, 성사 후(MATCHED) 취소는 신규 `/cancel-match`를 사용하세요.

---

## 2. 새 알림 타입 `match_released` 처리

- 알림 목록(`GET /notifications`)의 `type` 필드에 `"match_released"`가 새로 내려갑니다.
- **취소한 본인에게는 알림이 가지 않고, 상대방에게만** 발송됩니다.
- 분기 처리가 없으면 알림이 빈칸/에러로 표시될 수 있으니 **문구 매핑 추가** 필요.
  - 예: "성사된 매칭이 상대에 의해 취소되었습니다."
- 이 알림의 `matchingId`로 상세를 열면 해당 매칭은 `CANCELLED_AFTER_MATCH` 상태입니다.

### `match_cancelled` 와 혼동 주의

| 알림 type | 의미 | 매칭 status |
|---|---|---|
| `match_cancelled` | 내가 보낸 요청이 **상대가 다른 사람과 성사**되어 자동 취소됨 | `CANCELLED` |
| `match_released` | **성사된 매칭을 당사자가 직접 취소**함 | `CANCELLED_AFTER_MATCH` |

두 케이스는 status·알림 type 모두 다르므로 화면 문구를 구분해 표시하세요.

---

## 3. 새 상태값 `CANCELLED_AFTER_MATCH` 처리

- 보낸/받은/성사 목록(`GET /api/matchings/sent`, `/received`, `/matched`)과 상세에서 이 상태가 내려올 수 있습니다.
- 성사 목록(`GET /api/matchings/matched`)은 `MATCHED`만 반환하므로, 취소된 매칭은 **목록에서 빠집니다.**
- 화면에 떠 있던 성사 매칭이 상대의 취소로 사라질 수 있으니, `match_released` 알림 수신 시 목록을 **refetch** 하거나 `CANCELLED_AFTER_MATCH`를 "취소됨"으로 표시하세요.

---

## 4. 재매칭 동작

- 취소 즉시 두 프로필 점유가 해제되어, 같은 상대 또는 다른 상대와 **바로 다시 매칭 요청 가능**합니다. (현재 거절 쿨다운 같은 제한 없음)
- 프로필 응답의 `matched` 값도 취소 후 `false`로 돌아갑니다.

> 추후 쿨다운(예: 동일 조합 N일 재요청 제한)이 추가될 수 있습니다. 정책 변경 시 별도 공지 예정.

---

## 안 바꿔도 되는 것

- 기존 API 엔드포인트·응답 스키마(필드 구조) 그대로.
- 기존 `MatchingResponse` 필드 구조 변경 없음 (`status` 값만 새로 추가됨).

---

## 한 줄 요약

**필수:** (1) `POST /api/matchings/{id}/cancel-match` 연동, (2) `match_released` 알림 문구 추가, (3) `CANCELLED_AFTER_MATCH` 상태 표시.
**주의:** `match_cancelled`(자동취소)와 `match_released`(당사자 취소)를 구분해 표시.
