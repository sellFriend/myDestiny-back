# 매물 폼 사진 업로드 가이드

> 작성일: 2026-06-02  
> 대상: 프론트엔드 팀  
> 관련 명세: [frontend-api-spec.md](./frontend-api-spec.md) §5.5

---

## 개요

폼 제출 후 `uploadToken`을 이용해 사진을 관리합니다.  
사진 업로드는 폼 제출(프로필 텍스트 저장)과 **별도 단계**로 이루어집니다.

```
POST /form/{madamId}               → { uploadToken }   ← 폼 제출
GET  /form/{uploadToken}/photos    → [{ id, url, displayOrder }]
POST /form/{uploadToken}/photos    → { id, url, displayOrder }
PUT  /form/{uploadToken}/photos/{photoId}  → { id, url, displayOrder }
```

---

## 전체 흐름

```
① 사용자가 폼 작성 완료 → "제출" 클릭
        ↓
② POST /form/{madamId}
   → { acquaintanceId, uploadToken, status }

        ↓ uploadToken 저장

③ (useKakaoPhoto=true였다면)
   서버가 카카오 프로필 사진을 자동으로 displayOrder=0 으로 등록
   ※ 클라이언트 별도 처리 불필요

        ↓ 추가 사진이 있다면

④ POST /form/{uploadToken}/photos  (파일당 1회, 병렬 가능)
   → { id, url, displayOrder }

        ↓ 사진 교체가 필요하다면

⑤ GET  /form/{uploadToken}/photos          ← 현재 사진 목록 + id 확인
   PUT  /form/{uploadToken}/photos/{photoId} ← 교체 (구 파일 자동 삭제)
```

> 사진이 없으면 ②번만 호출하고 끝내도 됩니다.

---

## API 명세

### 사진 목록 조회

```
GET /destiny/form/{uploadToken}/photos
```

> 현재 등록된 사진과 photoId를 확인할 때 사용합니다.  
> 교체(PUT) 전에 호출해 photoId를 얻어야 합니다.

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": "uuid", "url": "https://cdn.example.com/photos/1.jpg", "displayOrder": 0 },
    { "id": "uuid", "url": "https://cdn.example.com/photos/2.jpg", "displayOrder": 1 }
  ]
}
```

**Error**
- `404` 유효하지 않은 uploadToken

---

### 사진 추가 업로드

```
POST /destiny/form/{uploadToken}/photos
Content-Type: multipart/form-data
```

**Form Data**
- `file`: 이미지 파일

**제약**
| 항목 | 제한 |
|---|---|
| 허용 타입 | `image/jpeg`, `image/png`, `image/webp`, `image/gif` |
| 최대 크기 | 10MB (서버 multipart 설정 기준) |
| 최대 장수 | **1장** (카카오 프로필 사진 포함) — 추후 여러 장 확장 예정 |

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "url": "https://cdn.example.com/photos/xxx.jpg",
    "displayOrder": 1
  }
}
```

**Error**
- `400` 이미지 파일이 아닌 경우 / 이미 최대 장수(1장)에 도달한 경우
- `404` 유효하지 않은 uploadToken

---

### 사진 교체

```
PUT /destiny/form/{uploadToken}/photos/{photoId}
Content-Type: multipart/form-data
```

> 기존 파일을 스토리지에서 **삭제**하고 새 파일로 대체합니다.  
> `photoId`는 목록 조회(`GET /photos`) 또는 업로드 응답(`POST /photos`)의 `id` 값입니다.

**Form Data**
- `file`: 새 이미지 파일

**Response** `200`
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "uuid",
    "url": "https://cdn.example.com/photos/new.jpg",
    "displayOrder": 0
  }
}
```

**Error**
- `400` 이미지 파일이 아닌 경우
- `403` uploadToken이 해당 사진의 소유자가 아닌 경우
- `404` 유효하지 않은 uploadToken 또는 photoId

---

## 사진 등록 결과 — 프로필 응답

마담이 지인 목록(`GET /destiny/api/profiles`)을 조회할 때 사진 URL이 자동으로 포함됩니다.  
별도 사진 조회 API 호출 불필요. (프로필 상세 `GET /destiny/api/profiles/{id}`도 `photoUrls` 포함)

```
GET /destiny/api/profiles
```

```json
{
  "data": [{
    "id": "uuid",
    "name": "김지인",
    "...": "...",
    "photoUrls": [
      "https://cdn.example.com/photos/1.jpg"
    ]
  }]
}
```

> `photoUrls`는 `displayOrder` 오름차순으로 정렬됩니다.

---

## 프론트엔드 구현 체크리스트

- [ ] `POST /form/{madamId}` 응답에서 `uploadToken` 저장 (state 또는 URL param)
- [ ] `useKakaoPhoto=true`인 경우 서버가 자동 등록하므로 클라이언트 별도 처리 없음
- [ ] 사진 선택 시 `POST /photos`로 업로드 → 응답의 `id` 보관
- [ ] 사진 교체 버튼: `GET /photos`로 목록 조회 후 해당 `id`로 `PUT /photos/{id}` 호출
- [ ] 최대 장수(현재 1장) 초과 시 서버에서 `400` 반환 — UI에서 업로드 버튼 비활성화 권장
- [ ] 허용 확장자 외 파일 선택 시 서버에서 `400` 반환 — `<input accept="image/jpeg,image/png,image/webp,image/gif">` 권장
