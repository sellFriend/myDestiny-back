# 모바일 사진 업로드 Network Error 진단 및 해결

## 증상

모바일 환경에서 폼 제출 시 사진 업로드가 실패하고 `Network Error`가 표시됨. 데스크톱에서는 정상 동작.

## 근본 원인

서버가 기기를 구분해서 막는 것이 아니라, **올라오는 파일 크기** 차이가 원인이다.

### 1. 모바일 사진은 용량 한도를 초과한다

- 모바일에서 올리는 사진 = 카메라 원본 → 보통 **8~25MB** (1200만~5000만 화소 JPEG)
- 데스크톱 테스트 시 올린 사진 = 스크린샷·캡처본 등 작은 이미지 → 대개 수백 KB~2MB
- 기존 한도가 **10MB**(`max-file-size` / `max-request-size`)였으므로 모바일 원본 사진이 이를 초과
- 즉 **"모바일에서만 에러"가 아니라 "큰 사진에서만 에러"** — 큰 사진이 거의 항상 모바일에서 나오기 때문에 모바일 문제처럼 보임

### 2. 왜 413이 아니라 "Network Error"였나

- 한도를 넘으면 Tomcat이 `MaxUploadSizeExceededException`을 던짐
- 그런데 Spring Boot 기본 `server.tomcat.max-swallow-size`가 **2MB**라서, 한도를 넘긴 요청의 남은 바디가 2MB보다 크면 Tomcat은 에러 응답을 보내는 대신 **TCP 연결을 그냥 끊어버림**
- 브라우저/axios는 정상 HTTP 응답(413/500)을 못 받고 **`Network Error`**로 인식
- 모바일 사진(15MB 등)은 잔여 바디가 수 MB라 항상 연결 리셋 → network error로 표시됨

### 3. (부차적) HEIC 포맷

- 아이폰 기본 포맷 `image/heic`는 현재 `validateImageType`에서 미허용
- 다만 대부분의 모바일 파일선택기가 업로드 시 JPEG로 자동 변환하므로 보통 안 걸림
- 걸리더라도 network error가 아니라 깔끔한 400("허용되지 않는 이미지 형식")이 떨어지므로 1차 원인 아님
- 일부 아이폰에서 "허용되지 않는 형식" 에러가 보고되면 그때 HEIC 허용/변환 처리 추가 검토

## 적용한 수정 (백엔드)

### `src/main/resources/application.properties`

```properties
# Multipart — 모바일 카메라 사진은 10MB를 쉽게 초과하므로 한도 상향
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB
# 한도 초과 시 Tomcat이 연결을 리셋(클라이언트엔 Network Error)하지 않고
# 남은 바디를 끝까지 읽어 정상 413 에러 응답을 돌려주도록 설정
server.tomcat.max-swallow-size=-1
```

### `src/main/java/com/mydestiny/global/exception/GlobalExceptionHandler.java`

`MaxUploadSizeExceededException` 핸들러 추가 → 한도 초과 시 413 + 친절한 메시지 반환:

```java
@ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(413).body(ApiResponse.fail("사진 용량이 너무 큽니다. 더 작은 사진을 올려주세요."));
}
```

> 컴파일 검증 완료 (`./gradlew compileJava` 성공).

## 남은 점검 항목 (운영 서버 — Nginx)

Nginx 설정은 이 레포에 없고 운영 서버에 직접 설치돼 있음. **Nginx의 `client_max_body_size` 기본값이 1MB**라서, 백엔드를 25MB로 올려도 Nginx에서 먼저 413으로 잘릴 수 있음 → 진짜 1차 병목일 가능성 있음.

### 현재 설정 확인

```bash
nginx -V 2>&1 | tr ' ' '\n' | grep conf-path
sudo nginx -T 2>/dev/null | grep -nE "client_max_body_size|proxy_pass|server_name|listen"
```

`client_max_body_size`가 안 보이면 기본값 1MB.

### 수정 (해당 site의 `server { }` 블록)

```nginx
server {
    # 백엔드 multipart 한도(25MB)와 맞춤. 약간 여유 둠
    client_max_body_size 30M;

    location /destiny/ {            # context-path와 일치
        proxy_pass http://127.0.0.1:8888;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 모바일 회선은 느려 타임아웃 가능 — 넉넉히 (선택)
        proxy_read_timeout    300s;
        proxy_send_timeout    300s;
        proxy_request_buffering off;
    }
}
```

핵심은 **`client_max_body_size`** 하나. 나머지는 느린 모바일 회선 안정성 보조용(선택).

### 적용

```bash
sudo nginx -t        # 문법 검사 (반드시 먼저)
sudo systemctl reload nginx
```

## 용량 한도 3중 게이트 요약

업로드 경로의 모든 단계가 통과해야 하며, **각 한도는 안쪽(Spring) ≤ 바깥쪽(Nginx) 순서로 커야** 에러가 엉뚱한 데서 안 남.

| 구간 | 설정 | 상태 |
|------|------|------|
| Nginx | `client_max_body_size` → 30M | ⚠️ 서버에서 확인/수정 필요 (기본 1MB면 여기서 막힘) |
| Tomcat swallow | `server.tomcat.max-swallow-size=-1` | ✅ 수정됨 |
| Spring multipart | `max-file-size=20MB` / `max-request-size=25MB` | ✅ 수정됨 |

## 근본적 개선 (선택, 프론트 작업)

20MB도 넘는 초고화질 사진이라면 여전히 막힘. 가장 안정적인 해법은 **프론트엔드에서 업로드 전 이미지 리사이즈/압축**하는 것. (프론트 레포는 이 저장소에 없음)
