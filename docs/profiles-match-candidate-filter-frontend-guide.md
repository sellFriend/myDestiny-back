# 매칭 요청 보낼 친구 선택 — 프론트 대응 가이드

## 배경

매칭 요청 **걸기는 한 사람한테만** 가능합니다(보낸 요청 1건 제한). 따라서 이미 요청을 보낸 친구나 이미 매칭이 성사된 친구는 **요청을 새로 걸 대상에서 빠져야** 합니다.

내 친구 목록과 "요청 보낼 친구 선택"은 **같은 엔드포인트(`GET /api/profiles`)** 를 씁니다. 엔드포인트를 나누는 대신, 응답에 프로필별 매칭 상태 플래그를 함께 내려주니 **프론트가 화면별로 필터**하면 됩니다.

---

## 응답 변경 — `GET /api/profiles`

각 친구 프로필 항목에 플래그 한 개가 추가되었습니다.

```json
{
  "id": "...",
  "name": "...",
  "matched": false,            // (기존) 매칭 성사 여부 — MatchingStatus.MATCHED
  "hasOutgoingRequest": true,  // (신규) 이 친구가 보낸 매칭 요청이 진행 중(PENDING)인지
  "...": "..."
}
```

| 필드 | 의미 |
|---|---|
| `matched` | 이 친구가 이미 매칭 성사됨 |
| `hasOutgoingRequest` | 이 친구가 누군가에게 요청을 보낸 상태(PENDING) |

> 두 플래그 모두 **그 친구 프로필 기준**입니다. 받은 요청(INCOMING)은 이 필터에 포함하지 않습니다 — 받기는 여러 건 가능하므로 요청 걸기에 지장이 없습니다.

---

## 프론트 필수 작업

### 1. 내 친구 탭

- **그대로 전체 표시.** 플래그로 거르지 않습니다.
- 원하면 `matched` / `hasOutgoingRequest`를 뱃지("매칭됨", "요청 보냄")로 활용 가능.

### 2. 매칭 요청 보낼 친구 선택 화면

- 아래 조건인 친구를 **목록에서 제외**:

```js
const selectable = profiles.filter(p => !p.matched && !p.hasOutgoingRequest);
```

- 즉 **OUTGOING(보낸 요청 PENDING)과 MATCHED만 제외**됩니다.

---

## 안 바꿔도 되는 것

- 엔드포인트 그대로 (`GET /api/profiles` 하나). 신규 API 없음.
- 기존 응답 필드·구조 그대로, `hasOutgoingRequest` 한 개만 추가.
- DB 마이그레이션 없음.

---

## 한 줄 요약

**선택 화면에서 `matched || hasOutgoingRequest`인 친구만 숨기면 끝.** 내 친구 탭은 그대로.
