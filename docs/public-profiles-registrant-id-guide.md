# 공개 프로필 응답에 주선자 아이디 추가 (프론트 전달용)

`GET /api/profiles/public` 응답의 각 프로필에 **주선자 아이디(`registrantId`)** 필드가 추가되었습니다.
해당 프로필을 등록한 주선자(Registrant)의 사용자 ID 값입니다.

---

## 1. 엔드포인트 — `GET /api/profiles/public`

| 항목 | 내용 |
|---|---|
| 인증 | 필요 (로그인 사용자) |
| 쿼리 파라미터 | `registrantId`(선택), `gender`(선택) |

쿼리 파라미터는 기존과 동일합니다.
- `registrantId`: 특정 주선자가 등록한 프로필만 필터링
- `gender`: 성별 필터링

---

## 2. 응답 변경 사항

각 프로필 객체에 `registrantId` 한 개 필드가 **추가**되었습니다. (기존 필드는 변경 없음)

| 필드 | 타입 | 설명 |
|---|---|---|
| `registrantId` | string | 프로필을 등록한 주선자의 사용자 ID |

### 응답 예시
```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "프로필 ID",
      "registrantId": "주선자 사용자 ID",
      "name": "홍길동",
      "age": 24,
      "gender": "MALE",
      "isStudent": true,
      "schoolName": "OO대학교",
      "major": "컴퓨터공학과",
      "occupation": null,
      "mbti": "ENFP",
      "hobby": "등산",
      "introduction": "안녕하세요",
      "firstPhotoUrl": "https://…"
    }
  ]
}
```

---

## 3. 프론트 활용
- 카드/프로필에서 "누가 등록했는지"를 구분하거나, 동일 주선자의 다른 프로필로 이어주는 데 사용할 수 있습니다.
- `registrantId`를 그대로 `GET /api/profiles/public?registrantId=…` 의 필터 값으로 넘겨, 같은 주선자가 올린 프로필 목록을 조회할 수 있습니다.
