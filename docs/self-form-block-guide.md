# 폼 화면 본인(마담) 차단 — 프론트 연동 가이드

My Destiny Server · 폼 링크 유효성 확인 흐름

## 1. 개요

마담이 **자신이 만든 폼 링크**(`/form/{madamId}`)로 들어와 로그인한 경우, 본인 폼에는 등록할 수 없으므로 서버가 진입을 차단한다.

차단이 동작하려면 프론트가 폼 화면에서 `GET /form/{madamId}` 호출 시 **로그인 상태면 토큰을 함께 보내야** 한다. 토큰이 없으면 서버는 요청자가 누구인지 알 수 없어 차단할 수 없다(공개 엔드포인트로 통과).

> 카카오 로그인 자체는 앱 전역 로그인이라 madamId가 실려 오지 않는다. 따라서 "로그인 그 순간"이 아니라 **로그인 후 폼 화면으로 돌아와 재검증하는 시점**에 차단한다.

## 2. 차단 시점 (엔드포인트)

| 동작 | 엔드포인트 | 비고 |
| --- | --- | --- |
| 폼 링크 유효성 확인 | `GET /form/{madamId}` | 본 가이드 대상 — 진입 시점 차단 |
| 폼 제출 | `POST /form/{madamId}` | 최종 방어선 (기존부터 차단) |

## 3. 프론트 요청 규칙

- 폼 화면에서 `GET /form/{madamId}` 호출 시, **로그인 상태(accessToken 보유)면** 헤더 추가:

  ```http
  Authorization: Bearer <accessToken>
  ```

- 비로그인이면 헤더 없이 호출 (기존과 동일, 정상 통과).
- 로그인하고 폼 화면으로 **돌아온 뒤에도 이 호출이 한 번 일어나야** 한다(폼 화면 mount 시 재검증). 그래야 토큰이 실려 차단이 걸린다.

## 4. 응답 형식

| 항목 | 값 |
| --- | --- |
| Content-Type | `application/json` |
| Body 스키마 | `{ success: boolean, message: string, data: null }` |

## 5. 케이스별 응답 (HTTP 상태 코드로 구분)

| HTTP 상태 | `message` | 의미 | 프론트 처리 |
| --- | --- | --- | --- |
| `200` | `유효한 폼 링크입니다.` | 정상 | 폼 표시 |
| `400` | `본인의 폼에는 등록할 수 없습니다.` | **본인 폼 (차단)** | 폼 숨기고 안내 후 홈 등으로 이동 |
| `404` | `유효하지 않은 폼 링크입니다.` | 잘못된 링크 | 기존 에러 처리 |

**본인 폼 차단 응답 예시**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "success": false,
  "message": "본인의 폼에는 등록할 수 없습니다.",
  "data": null
}
```

> **프론트엔드 처리 가이드** — 상태 코드가 `400` 이면 본인 폼이므로 `message` 를 그대로 노출하고 폼 진입을 중단한다. `404` 는 잘못된 링크로 기존 처리를 따른다.

## 6. 처리 경로

`/form/**` 체인에 `JwtAuthenticationFilter` 가 적용되어, `Authorization: Bearer` 헤더가 있으면 principal(userId)이 채워진다. `FormController.validateForm` 이 이 userId 를 `AcquaintanceService.getFormState(madamId, userId)` 에 넘기고, `madamId.equals(userId)` 면 `BusinessException(400)` 을 던진다. 모든 `BusinessException` 은 `GlobalExceptionHandler` 에서 `ApiResponse.fail(message)` 로 변환된다.
