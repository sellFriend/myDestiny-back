# 폼 변경 가이드 — 학생/학교/전공 필드 추가 (프론트 전달용)

매물(친구) 폼에 **학생 여부 분기**가 추가되었습니다.
학생이면 `학교 / 전공`, 비학생이면 `직업`을 받습니다.

대상 화면: 카카오 로그인 후 친구가 본인 정보를 작성/수정하는 폼.

---

## 1. 제출 API — `POST /destiny/form/{madamId}`

기존 요청 바디에 **필드 3개가 추가**되었습니다. (`useKakaoPhoto`, `name`, `age`, `gender`, `intro`, `mbti`, `hobbies`, `phoneNumber`, `kakaoId`, `instagramId` 는 변경 없음)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `isStudent` | boolean | **필수** | 학생이면 `true`, 아니면 `false` |
| `schoolName` | string(≤100) | 학생일 때 필수 | 학교명 |
| `major` | string(≤100) | 학생일 때 필수 | 전공 |
| `job` | string(≤100) | 비학생일 때 필수 | 직업 (기존 필드) |

### 입력 규칙 (서버가 강제)
- `isStudent`는 **항상 보내야 함** (누락 시 400).
- `isStudent: true` → `schoolName`, `major` 둘 다 비어있지 않아야 함. (`job`은 안 보내도 됨)
- `isStudent: false` → `job` 이 비어있지 않아야 함. (`schoolName`, `major`는 안 보내도 됨)
- 반대편 필드는 `null` 또는 생략 권장. (보내도 무시되진 않고 저장되니, UI에서 분기 전환 시 반대편 값은 비워서 보내는 걸 권장)

### 요청 예시

학생인 경우:
```json
{
  "useKakaoPhoto": true,
  "name": "홍길동",
  "age": 24,
  "gender": "MALE",
  "isStudent": true,
  "schoolName": "OO대학교",
  "major": "컴퓨터공학과",
  "job": null,
  "intro": "안녕하세요",
  "mbti": "ENFP",
  "hobbies": "등산",
  "phoneNumber": "01012345678",
  "kakaoId": "gildong",
  "instagramId": "gildong.k"
}
```

비학생인 경우:
```json
{
  "useKakaoPhoto": true,
  "name": "홍길동",
  "age": 30,
  "gender": "MALE",
  "isStudent": false,
  "schoolName": null,
  "major": null,
  "job": "백엔드 개발자",
  "intro": "안녕하세요",
  "mbti": "ENFP",
  "hobbies": "등산",
  "phoneNumber": "01012345678",
  "kakaoId": "gildong",
  "instagramId": "gildong.k"
}
```

### 성공 응답 (변경 없음)
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "acquaintanceId": "…",
    "uploadToken": "…",
    "status": "PENDING_APPROVAL"
  }
}
```

---

## 2. 폼 진입(prefill) API — `GET /destiny/form/{madamId}`

기존 작성분이 있으면 `data.draft`에 값을 내려주는데, 여기에도 **3개 필드가 추가**되었습니다.
재방문/수정 화면에서 이 값들로 폼을 채워주세요. (안 채우면 사용자가 학교/전공을 다시 입력해야 검증을 통과함)

```json
{
  "success": true,
  "message": "유효한 폼 링크입니다.",
  "data": {
    "draft": {
      "acquaintanceId": "…",
      "uploadToken": "…",
      "status": "DRAFT",
      "name": "홍길동",
      "age": 24,
      "gender": "MALE",
      "isStudent": true,
      "schoolName": "OO대학교",
      "major": "컴퓨터공학과",
      "job": null,
      "intro": "…",
      "mbti": "ENFP",
      "hobbies": "등산",
      "phoneNumber": "01012345678",
      "kakaoId": "gildong",
      "instagramId": "gildong.k",
      "photoUrls": ["…"]
    }
  }
}
```

- `draft`가 `null`이면 신규 작성 상태 → `isStudent`는 화면에서 기본값(예: false 또는 미선택)으로 시작.
- `draft`가 있으면 `isStudent` 값에 따라 학교·전공 / 직업 입력 영역을 토글해서 prefill.

---

## 3. 검증 실패 응답 (400)

서버 검증 실패 시 형태:
```json
{ "success": false, "message": "<필드>: <메시지>", "data": null }
```

발생 케이스:
| 상황 | message 예시 |
|---|---|
| `isStudent` 미전송 | `isStudent: must not be null` |
| 학생인데 학교/전공 비었거나, 비학생인데 직업 비었음 | `occupationInfoValid: 학생은 학교·전공을, 비학생은 직업을 입력해야 합니다` |
| 학교/전공/직업 100자 초과 | `schoolName: ...` / `major: ...` / `job: ...` |

> `message`는 표시용으로만 사용하고, 분기 검증은 프론트에서도 동일하게(학생→학교·전공, 비학생→직업) 1차로 걸어 주세요. 서버는 마지막 방어선입니다.

---

## 4. 프론트 체크리스트
- [ ] 폼에 "학생 여부" 토글/라디오 추가 (`isStudent`)
- [ ] 학생 선택 시: 학교명 / 전공 입력 노출 + 필수 처리, 직업 숨김
- [ ] 비학생 선택 시: 직업 입력 노출 + 필수 처리, 학교/전공 숨김
- [ ] 토글 전환 시 반대편 필드 값 비워서(`null`) 전송
- [ ] prefill 응답의 `isStudent`/`schoolName`/`major`로 초기 상태 복원
