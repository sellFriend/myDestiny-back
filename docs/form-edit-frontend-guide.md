# 폼 수정(재작성) 흐름 — 프론트 구현 가이드

주선자(마담)가 이미 등록된 매물 카드를 **다시 수정하도록 요청**하고, 매물(친구)이 폼을 고쳐 재제출하는 흐름을 구현하기 위한 가이드입니다.

> 모든 경로 prefix는 `/destiny` 입니다. (예: `/destiny/api/profiles/{id}/request-edit`)

---

## 1. 전체 흐름

```
[주선자] 카드 수정 요청 (request-edit)        [매물] 폼 수정 후 재제출 (submit)
        │                                              │
        ▼                                              ▼
  매칭에 묶여있지 않으면(아래 조건 통과)           DRAFT/PENDING_APPROVAL 인 카드만
  카드 → DRAFT 로 전환                            재제출 → PENDING_APPROVAL 로 복귀
  + 매물에게 edit_requested 알림                  + 주선자에게 form_submitted 알림
```

- 공개(`PUBLISHED`)된 카드는 매물이 임의로 다시 제출할 수 없습니다. **반드시 주선자가 수정 요청을 먼저 보내야** 카드가 편집 가능(`DRAFT`) 상태가 됩니다.
- 매물이 여는 폼 링크는 `/destiny/form/{madamId}` 입니다. 이 URL은 `GET /destiny/api/profiles/my-form` 응답의 `formUrl`로 조회할 수 있는 마담 영구 링크이며, 그대로 매물에게 전달하면 됩니다.

---

## 2. 상태별 동작 요약

| 카드 상태 | 매물 자체 재제출 | 주선자 수정 요청(request-edit) |
|---|---|---|
| `DRAFT` | ✅ 가능 | — (이미 편집 상태) |
| `PENDING_APPROVAL` | ✅ 가능 | ✅ 가능 → `DRAFT` |
| `PUBLISHED` | ❌ 불가 | ✅ 가능(매칭에 묶여있지 않을 때) → `DRAFT` |
| `REJECTED` | ❌ 불가 | ❌ 불가 (수정 경로 없음 — 거절은 종료 상태) |

---

## 3. 주선자 — 폼 수정 요청

```
POST /destiny/api/profiles/{id}/request-edit
Authorization: Bearer {accessToken}
```

- `{id}` = 수정 요청할 프로필(카드) ID
- 성공 시 카드가 `DRAFT`로 바뀌고 매물에게 `edit_requested` 알림이 갑니다.

**성공 Response** `200`
```json
{ "success": true, "message": "OK", "data": null }
```

### 3.1 매칭 상태 확인 (수정 요청 전 정리 필요)

수정 요청 전에 해당 카드의 매칭을 먼저 정리해야 합니다. 서버가 아래 순서로 검사하며, 하나라도 걸리면 `409 Conflict` + 해당 메시지를 반환합니다. 전부 안 걸려야(=매칭에 묶여있지 않아야) 수정 요청이 통과합니다.

| HTTP | message | 의미 | 프론트 안내 |
|---|---|---|---|
| `409` | `이미 매칭이 진행 중이거나 성사된 프로필은 수정 요청할 수 없습니다.` | 상대가 수락했거나 동의 진행 중/성사(`MATCHED`) | 수정 불가임을 알림. 매칭을 끝낸 카드가 아니면 수정할 수 없음 |
| `409` | `받은 매칭 요청을 모두 거절한 뒤 수정 요청이 가능합니다.` | 받은 `PENDING` 요청이 남아 있음 | "받은 요청"으로 유도 → 전부 거절 후 재시도 |
| `409` | `보낸 매칭 요청을 모두 취소한 뒤 수정 요청이 가능합니다.` | 보낸 `PENDING` 요청이 남아 있음 | "보낸 요청"으로 유도 → 전부 취소 후 재시도 |
| `409` | `승인 대기 또는 공개 상태에서만 수정 요청할 수 있습니다.` | 카드가 `PENDING_APPROVAL`/`PUBLISHED`가 아님 | 현재 상태에선 수정 요청 불가 |

### 3.2 요청 정리에 쓰는 엔드포인트

위 확인에 걸렸을 때 매물 카드에 묶인 요청을 정리하도록 안내하세요.

- 받은 요청 목록: `GET /destiny/api/matchings/received`
- 받은 요청 거절: `POST /destiny/api/matchings/{id}/reject` (body: `{ "reason": "..." }` 선택)
- 보낸 요청 목록: `GET /destiny/api/matchings/sent`
- 보낸 요청 취소: `POST /destiny/api/matchings/{id}/cancel`

> 거절·취소는 `PENDING` 상태에서만 가능합니다. 전부 정리한 뒤 `request-edit`를 재시도하면 통과합니다.

---

## 4. 매물 — 폼 재제출

기존 폼 제출과 동일한 엔드포인트입니다.

```
POST /destiny/form/{madamId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

- 기존 작성분이 있으면 **수정(재제출)** 으로 처리되고, 카드 상태가 `PENDING_APPROVAL`로 복귀합니다.
- 재제출은 카드가 **`DRAFT`(주선자의 수정 요청을 받음) 또는 `PENDING_APPROVAL`(아직 승인 전)** 일 때만 가능합니다.

**재제출 차단 Error**

| HTTP | message | 의미 |
|---|---|---|
| `409` | `주선자에게 폼 수정 요청을 받은 카드만 수정할 수 있습니다.` | 카드가 `DRAFT`/`PENDING_APPROVAL`이 아님(예: `PUBLISHED`) |

> 매물 화면에서 `PUBLISHED` 카드 수정 진입 시, 이 에러를 받으면 "주선자에게 수정 요청을 받아야 합니다" 안내를 띄우세요.

**성공 Response** `200`
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

---

## 5. 권장 UX 시나리오

1. 주선자가 카드 상세에서 **"수정 요청"** 버튼 클릭 → `request-edit` 호출
2. `409`(매칭 상태) 응답 시 메시지에 맞춰 안내:
   - 매칭 성사/진행 → 비활성화 + 사유 표시
   - 받은/보낸 요청 → 해당 요청 목록으로 이동시켜 정리 유도
3. 성공 시 매물에게 폼 링크 전달 (알림 `edit_requested`도 발송됨)
4. 매물이 폼 링크로 진입 → 기존 값 prefill → 수정 후 제출 → `PENDING_APPROVAL`
5. 주선자가 다시 승인하면 `PUBLISHED`로 공개
