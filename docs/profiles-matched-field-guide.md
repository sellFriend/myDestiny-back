# 프로필 응답에 매칭 성사 여부(`matched`) 추가 (프론트 전달용)

아래 두 API 응답의 각 프로필에 **매칭 성사 여부(`matched`)** 필드가 추가되었습니다.
해당 프로필이 **매칭 성사(`MATCHED`) 상태인 매칭에 엮여 있으면 `true`**, 아니면 `false`입니다.

- `GET /api/profiles` — 내가 주선자로 등록한 친구(매물) 목록
- `GET /api/profiles/{id}` — 프로필 상세

> ⚠️ 새 컬럼이 아니라 매칭 데이터에서 계산되는 값입니다. DB 마이그레이션은 없습니다.

---

## 1. `matched` 의 의미

| 값 | 의미 |
|---|---|
| `true` | 양쪽 당사자가 모두 동의하여 **매칭이 최종 성사된** 상태(`MatchingStatus.MATCHED`) |
| `false` | 그 외 전부 (요청 진행 중·수락만 된 상태·거절·만료·매칭 이력 없음 등) |

- "진행 중"이나 "수락됨" 같은 중간 단계는 `false`입니다. 오직 **최종 성사**만 `true`입니다.
- 한 프로필이 요청자(requester) 쪽이든 수신자(target) 쪽이든, 성사된 매칭에 속해 있으면 `true`입니다.

---

## 2. `GET /api/profiles` — 내가 등록한 친구 목록

| 항목 | 내용 |
|---|---|
| 인증 | 필요 (로그인 사용자 = 주선자) |
| 응답 | 배열 (각 원소가 등록한 친구 카드) |

각 카드 객체에 `matched` 한 개 필드가 **추가**되었습니다. (기존 필드는 변경 없음)

| 필드 | 타입 | 설명 |
|---|---|---|
| `matched` | boolean | 매칭 성사 여부 (`MATCHED`이면 `true`) |

### 응답 예시
```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "프로필 ID",
      "name": "홍길동",
      "age": 24,
      "gender": "MALE",
      "job": "컴퓨터공학과",
      "intro": "안녕하세요",
      "mbti": "ENFP",
      "hobbies": "등산",
      "registrationStatus": "PUBLISHED",
      "visibility": "PUBLIC",
      "matched": true,
      "verifiedAt": "2026-06-01T12:00:00",
      "photoUrls": ["https://…"]
    }
  ]
}
```

---

## 3. `GET /api/profiles/{id}` — 프로필 상세

| 항목 | 내용 |
|---|---|
| 인증 | 필요 (로그인 사용자) |
| 응답 | 단일 객체 |

응답 객체에 `matched` 한 개 필드가 **추가**되었습니다. (기존 필드는 변경 없음)
소유자/당사자/공개 조회 어느 경우든 동일한 값이 내려갑니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `matched` | boolean | 매칭 성사 여부 (`MATCHED`이면 `true`) |

### 응답 예시
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "프로필 ID",
    "registrantId": "주선자 사용자 ID",
    "registrantNickname": "주선자 닉네임",
    "status": "PUBLISHED",
    "visibility": "PUBLIC",
    "matched": true,
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
    "kakaoId": "…",
    "instagramId": "…",
    "subjectPhone": "…",
    "photoUrls": ["https://…"],
    "createdAt": "2026-06-01T12:00:00",
    "updatedAt": "2026-06-01T12:00:00"
  }
}
```

> `status`(프로필 등록 단계: `DRAFT`/`PUBLISHED`/…)와 `matched`(매칭 성사 여부)는 **서로 다른 축**입니다.
> 예: 프로필이 `PUBLISHED`이면서 동시에 `matched: true`일 수 있습니다.

---

## 4. 프론트 활용
- 내 매물 목록에서 "성사됨" 배지/상태를 표시할 때 `matched`를 사용하세요.
- `matched: true`인 카드는 더 이상 새 매칭 요청을 받지 않으므로, 요청 버튼을 숨기거나 비활성화하는 데 활용할 수 있습니다.
- 참고: 공개 목록 `GET /api/profiles/public`에는 성사·진행 중(점유) 프로필이 애초에 노출되지 않으므로, 그 목록에는 `matched` 필드가 없습니다.
