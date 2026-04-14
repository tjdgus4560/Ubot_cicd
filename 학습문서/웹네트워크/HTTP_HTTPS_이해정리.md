# HTTP/HTTPS 학습 문서

## 문서 목적
- 이 문서는 `HTTP 완벽 가이드`에서 반복적으로 강조하는 핵심 개념을 기준으로, 현재 `Ubot` 프로젝트를 올바르게 구현하고 배포하기 위해 꼭 알아야 할 HTTP/HTTPS 지식을 실전형으로 재구성한 문서다.
- 목표는 책 전체를 요약하는 것이 아니라, 이 프로젝트에서 바로 부딪히는 문제를 이해하고 판단할 수 있게 만드는 것이다.

## 이 프로젝트에서 HTTP/HTTPS를 왜 제대로 알아야 하는가
이 프로젝트는 단순한 CRUD 웹앱이 아니다. 아래 요소들이 한 번에 얽혀 있다.

- 브라우저와 프론트엔드 간 페이지 요청
- 프론트엔드와 백엔드 간 REST API 통신
- `Authorization: Bearer` 기반 Access Token 인증
- 쿠키 기반 Refresh Token 재발급
- OAuth2 로그인 리다이렉트
- CORS 및 `withCredentials`
- Nginx Reverse Proxy
- 도메인, SSL/TLS, HTTPS
- 외부 거래소 WebSocket(`wss://api.upbit.com/websocket/v1`)

즉, HTTP를 모르면 "왜 안 되는지"를 설명할 수 없고, HTTPS를 모르면 "왜 운영에서는 위험한지"를 판단할 수 없다.

---

## 1. 먼저 머릿속에 넣어야 할 전체 흐름

### 브라우저 사용자의 일반 요청 흐름
```text
브라우저
  -> https://www.ubot.site
  -> Nginx
  -> Frontend 또는 /api 경로는 Spring Boot
  -> 응답 반환
```

### 로그인 및 토큰 흐름
```text
브라우저
  -> 백엔드 /oauth2/authorization/kakao
  -> 카카오 로그인
  -> 백엔드 /login/oauth2/code/kakao
  -> 백엔드가 Refresh Token 쿠키 설정
  -> 프론트엔드로 redirect
  -> 프론트엔드가 /token 호출로 Access Token 재발급
  -> 이후 API 요청 시 Authorization 헤더에 Access Token 첨부
```

### 실시간 시세 흐름
```text
브라우저
  -> wss://api.upbit.com/websocket/v1
  -> 실시간 ticker 수신
```

이 세 흐름을 분리해서 이해하면, 오류도 어디서 생기는지 분리해서 볼 수 있다.

---

## 2. HTTP의 본질: 요청과 응답 메시지

HTTP는 "클라이언트가 요청(Request)을 보내고 서버가 응답(Response)을 돌려주는 규약"이다.

### 요청의 핵심 구성요소
- 시작줄: 메서드, 경로, HTTP 버전
- 헤더: 인증, 콘텐츠 타입, 쿠키, 캐시 정책 등 메타데이터
- 본문: JSON, 폼 데이터, 파일 등 실제 데이터

예시:
```http
GET /api/token HTTP/1.1
Host: api.example.com
Cookie: refreshToken=...
```

### 응답의 핵심 구성요소
- 상태줄: HTTP 버전, 상태 코드
- 헤더: 콘텐츠 타입, 캐시, 쿠키 설정, 리다이렉트 위치 등
- 본문: JSON, HTML, 파일 등

예시:
```http
HTTP/1.1 200 OK
Content-Type: application/json
Set-Cookie: refreshToken=...
```

### 이 프로젝트에서 중요한 이유
- Access Token은 보통 요청 헤더 `Authorization`에 실린다.
- Refresh Token은 응답 헤더 `Set-Cookie`로 내려가고, 이후 요청 시 쿠키로 다시 올라온다.
- OAuth2 로그인 후 화면 이동은 응답 본문이 아니라 `302 Redirect + Location` 또는 `sendRedirect()`로 처리된다.

---

## 3. URL, URI, 경로 설계

책에서 매우 중요하게 다루는 포인트 중 하나가 "리소스를 어떻게 식별하느냐"다.

### 최소한 알아야 할 것
- `URI`: 자원을 식별하는 개념 전체
- `URL`: 자원의 위치까지 포함한 식별자
- 경로(path), 쿼리(query), 스킴(scheme)을 구분해야 한다

예시:
- `https://www.ubot.site/api/member/me`
  - 스킴: `https`
  - 호스트: `www.ubot.site`
  - 경로: `/api/member/me`

### 이 프로젝트 기준으로 보면
- 현재 백엔드는 `server.servlet.context-path: /api`를 사용한다.
- 즉 실제 엔드포인트는 `/member/me`가 아니라 `/api/member/me`처럼 동작한다.
- 배포 시 Nginx가 `/api`를 Spring Boot로 넘기면, 프론트는 하나의 도메인 아래에서 API를 사용할 수 있다.

### 실수 포인트
- 프론트엔드의 `NEXT_PUBLIC_API_BASE_URL`이 `/api`를 포함하는지 안 하는지 헷갈리면 404가 자주 난다.
- 리다이렉트 URL, OAuth2 callback URL, 프록시 경로가 서로 다르면 로그인 흐름이 깨진다.

---

## 4. HTTP 메서드와 의미

대표 메서드:
- `GET`: 조회
- `POST`: 생성, 처리 요청
- `PUT`: 전체 수정
- `PATCH`: 부분 수정
- `DELETE`: 삭제

### 왜 단순 암기가 아니라 의미가 중요할까
HTTP는 메서드에 기대하는 의미가 있다.

- 안전성(safe): 서버 상태를 바꾸지 않는가
- 멱등성(idempotent): 여러 번 보내도 결과가 같은가

예시:
- `GET /api/token`은 조회처럼 보이지만 실제로는 새 Access Token 발급이라는 상태 변화가 있다.
- 설계상 `POST`가 더 자연스러울 수 있지만, 현재 구현은 `GET /token`이다.

### 이 프로젝트에서 얻어야 할 감각
- "동작한다"와 "HTTP 의미에 맞다"는 다르다.
- API를 설계할 때는 메서드 의미와 캐시, 보안, 브라우저 동작까지 함께 봐야 한다.

---

## 5. 상태 코드: 문제를 읽는 첫 번째 언어

자주 보는 상태 코드:
- `200 OK`: 정상
- `201 Created`: 생성 성공
- `204 No Content`: 본문 없는 성공
- `301/302`: 리다이렉트
- `400 Bad Request`: 요청 형식 문제
- `401 Unauthorized`: 인증 실패 또는 토큰 문제
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 경로 없음
- `500 Internal Server Error`: 서버 내부 오류
- `502/504`: 프록시 또는 upstream 장애

### 이 프로젝트에서 특히 중요
- JWT 만료 시 `401`
- 잘못된 토큰 형식/검증 실패 시 `400`, `403` 계열이 섞일 수 있음
- Nginx 뒤에 배치하면 애플리케이션은 멀쩡한데 프록시 설정 때문에 `502 Bad Gateway`가 날 수 있음

### 실전 팁
- 브라우저 개발자도구에서 "어느 URL에 어떤 상태 코드가 왔는지"를 먼저 본다.
- 401이면 인증 계층 문제, 404면 경로 문제, 502면 프록시/포트/서버 기동 문제를 우선 의심한다.

---

## 6. 헤더: HTTP 동작을 결정하는 메타데이터

이 프로젝트에서 반드시 익숙해져야 할 헤더:

- `Authorization`
- `Content-Type`
- `Accept`
- `Cookie`
- `Set-Cookie`
- `Origin`
- `Access-Control-Allow-Origin`
- `Access-Control-Allow-Credentials`
- `Location`
- `Host`
- `X-Forwarded-Proto`, `X-Forwarded-For`, `X-Forwarded-Host`
- `Upgrade`, `Connection` (WebSocket/Nginx에서 중요)

### 프로젝트 적용 예시
- 프론트는 Axios 인터셉터에서 `Authorization: Bearer <token>`를 붙인다.
- 백엔드는 OAuth2 로그인 성공 후 `Set-Cookie`로 Refresh Token을 내려준다.
- `withCredentials: true`를 쓰면 쿠키 전달을 위해 CORS 헤더도 정확해야 한다.
- 프록시 뒤 HTTPS 환경에서는 원래 요청 스킴을 애플리케이션이 알 수 있도록 forwarded header 처리가 중요하다.

---

## 7. 쿠키와 세션, 그리고 이 프로젝트의 토큰 전략

### 쿠키란
서버가 `Set-Cookie` 응답 헤더로 내려주고, 브라우저가 이후 요청마다 자동으로 실어 보내는 작은 상태 정보다.

### 현재 프로젝트의 구조
- Access Token: 프론트에서 보관 후 `Authorization` 헤더에 직접 첨부
- Refresh Token: 쿠키로 저장하고 `/token` 호출 시 재사용

### 왜 이 구조를 이해해야 하나
같은 "토큰"이어도 전송 방식이 다르면 보안 특성과 브라우저 동작이 달라진다.

- 헤더 기반 토큰은 JS가 직접 다룬다.
- 쿠키 기반 토큰은 브라우저 정책(`SameSite`, `Secure`, `HttpOnly`) 영향을 받는다.

### 운영에서 꼭 알아야 할 쿠키 속성
- `HttpOnly`: JS에서 쿠키 접근 불가. XSS 대응에 중요.
- `Secure`: HTTPS에서만 전송.
- `SameSite`: 크로스 사이트 요청에 쿠키를 보낼지 결정.
  - `Lax`
  - `Strict`
  - `None` (`Secure`와 함께 사용해야 함)
- `Path`
- `Domain`
- `Max-Age` / `Expires`

### 현재 코드 기준 체크 포인트
- OAuth2 성공 핸들러에서 Refresh Token 쿠키에 `HttpOnly`, `Secure` 설정이 주석 처리되어 있다.
- 운영 HTTPS 환경이라면 이 부분은 반드시 다시 점검해야 한다.
- 프론트와 백엔드가 서로 다른 사이트로 운영되면 `SameSite=None; Secure`까지 검토해야 한다.
- 반대로 Nginx로 같은 사이트 아래 경로 기반(`/api`)으로 합치면 쿠키/CORS 복잡도가 크게 줄어든다.

---

## 8. 인증(Authentication)과 인가(Authorization)

구분:
- 인증: "너 누구냐?"
- 인가: "그걸 할 권한이 있냐?"

### 이 프로젝트에 대입
- 카카오 OAuth2 로그인은 사용자 신원을 확인하는 인증 단계
- JWT는 이후 요청에서 "이미 인증된 사용자"를 증명하는 수단
- 특정 API 접근 허용 여부는 Spring Security와 컨트롤러 보호가 담당

### 왜 HTTP 문맥에서 중요할까
인증 정보는 결국 HTTP 메시지 안에서 움직인다.

- OAuth2 로그인: 리다이렉트 기반
- Access Token: 요청 헤더 기반
- Refresh Token: 쿠키 기반

즉, 인증은 보안 개념이면서 동시에 HTTP 전달 방식의 문제다.

---

## 9. OAuth2 리다이렉트 흐름 이해

OAuth2에서 가장 많이 헷갈리는 부분은 "로그인 버튼을 누른 뒤 왜 여러 번 화면이 이동하느냐"다.

### 흐름
1. 브라우저가 백엔드 `/oauth2/authorization/kakao`로 이동
2. 백엔드가 카카오 인증 페이지로 리다이렉트
3. 사용자가 로그인/동의
4. 카카오가 백엔드 callback URL로 인가 코드를 전달
5. 백엔드가 토큰과 사용자 정보를 처리
6. 백엔드가 Refresh Token 쿠키를 설정하고 프론트 URL로 다시 리다이렉트

### 반드시 이해할 점
- OAuth2 로그인은 단순 AJAX 호출이 아니라 "브라우저 네비게이션"이다.
- 그래서 CORS 문제와 일반 API 문제, redirect 문제를 구분해서 봐야 한다.
- `redirect-uri`와 실제 서비스 도메인이 맞지 않으면 로그인은 거의 반드시 깨진다.

---

## 10. CORS와 브라우저 보안 정책

### CORS란
브라우저가 다른 출처(origin)로 보내는 요청을 통제하는 정책이다.

Origin은 보통 아래 세 가지의 조합으로 결정된다.
- 스킴(`http` / `https`)
- 호스트
- 포트

예시:
- `http://localhost:3000`
- `https://autric.site`
- `https://www.autric.site`

서로 하나라도 다르면 다른 origin이다.

### 현재 코드에서 보이는 설정
- `allowedOrigins`: `http://localhost:3000`, `https://autric.site`, `https://www.autric.site`
- `allowCredentials(true)`

### 여기서 꼭 알아야 하는 규칙
- `credentials`를 허용하면 `Access-Control-Allow-Origin: *`를 쓸 수 없다.
- 프론트에서 `withCredentials: true`를 쓰면 서버도 정확한 origin을 허용해야 한다.
- 쿠키를 사용하는 순간 CORS는 더 엄격하게 봐야 한다.

### 실전 포인트
- 로컬은 되는데 운영에서 안 되면 "도메인/포트/https 차이"부터 본다.
- 프론트와 API를 같은 도메인 아래로 묶으면 CORS 문제를 크게 줄일 수 있다.

---

## 11. HTTPS와 TLS: 왜 운영에서는 필수인가

HTTP는 평문 전송이다. HTTPS는 HTTP 위에 TLS를 얹어 암호화한 것이다.

### HTTPS가 해결하는 것
- 기밀성: 중간에서 내용을 못 읽게 함
- 무결성: 중간 변조를 어렵게 함
- 서버 인증: 내가 진짜 그 서버와 통신 중인지 확인

### 이 프로젝트에서 HTTPS가 특히 중요한 이유
- 로그인
- JWT 및 Refresh Token
- API 키 등록
- 개인정보와 주문 관련 데이터
- 브라우저 보안 기능 활성화

### HTTPS를 안 쓰면 생기는 문제
- 토큰 탈취 위험 증가
- `Secure` 쿠키 사용 불가
- 일부 브라우저 기능/보안 정책 제한
- 운영 신뢰도 하락

### 꼭 기억할 문장
운영에서 인증, 쿠키, 개인정보가 오가는데 HTTPS가 없으면 설계가 아니라 사고 대기 상태다.

---

## 12. Reverse Proxy와 Nginx

### Reverse Proxy란
클라이언트는 프록시(Nginx)와 통신하고, 프록시가 내부 서버(Spring Boot, Frontend 등)로 요청을 전달하는 구조다.

### 왜 두는가
- 외부 공개 진입점 단일화
- HTTPS 종료 지점 제공
- 라우팅 분기
- 정적 파일/캐시 처리
- 보안 헤더 및 업로드 제한 관리

### 프로젝트에 맞는 전형적 라우팅
- `/` -> Frontend
- `/api` -> Spring Boot
- 필요 시 `/fastapi` -> FastAPI

### 이때 꼭 알아야 할 것
- 외부는 443 하나만 열고 내부 3000, 8080, 8000은 닫아두는 구조가 안전하다.
- 프록시를 거치면 애플리케이션이 원래 요청의 스킴과 클라이언트 IP를 잃을 수 있어서 forwarded header 처리가 중요하다.
- 현재 백엔드는 `forward-headers-strategy: framework`를 사용하고 있어 프록시 환경 고려가 들어가 있다.

---

## 13. WebSocket과 HTTP의 관계

WebSocket은 처음 연결을 시작할 때 HTTP 핸드셰이크를 사용한다. 이후에는 일반 HTTP 응답/요청이 아니라 지속 연결 기반 양방향 통신으로 전환된다.

### 알아야 할 용어
- `ws://`: 평문 WebSocket
- `wss://`: TLS 적용 WebSocket

### 이 프로젝트에서의 사용
- 프론트는 업비트 외부 WebSocket에 직접 `wss://api.upbit.com/websocket/v1`로 연결한다.

### Nginx를 거칠 때 중요해지는 헤더
- `Upgrade: websocket`
- `Connection: Upgrade`

### 실수 포인트
- HTTPS 사이트에서 `ws://`를 쓰면 혼합 콘텐츠 문제로 막힐 수 있다.
- 프록시 설정이 부족하면 REST는 되는데 WebSocket만 끊기는 경우가 많다.

---

## 14. 캐시와 재검증 감각

`HTTP 완벽 가이드`에서 중요하게 다루는 주제 중 하나가 캐시다.

### 왜 지금 당장 크게 중요하냐
이 프로젝트는 시세/포트폴리오/주문 데이터처럼 "즉시성"이 필요한 데이터가 많다.

### 감각적으로 구분해야 할 것
- 강하게 캐시하면 안 되는 것
  - 사용자 정보
  - 주문 상태
  - 토큰 응답
  - 실시간 가격 데이터
- 캐시해도 되는 것
  - 정적 이미지
  - 정적 번들 JS/CSS
  - 변동이 적은 공개 메타데이터

### 실전 적용
- 인증 관련 응답은 캐시되면 안 된다.
- 정적 파일은 Nginx나 CDN에서 캐시 전략을 세울 수 있다.
- 실시간성 요구가 큰 데이터는 캐시보다 polling/WebSocket/짧은 TTL이 더 중요하다.

---

## 15. Content-Type과 데이터 포맷

서버와 클라이언트는 "무슨 형식의 데이터인지"를 합의해야 한다.

자주 쓰는 타입:
- `application/json`
- `application/x-www-form-urlencoded`
- `multipart/form-data`
- `text/html`

### 이 프로젝트에서 중요
- 대부분 API는 JSON 기반이다.
- 파일 업로드나 외부 API 연동이 생기면 `multipart/form-data` 또는 서명 요청 형식을 이해해야 한다.
- `Content-Type`이 틀리면 서버는 같은 데이터도 다르게 해석한다.

---

## 16. 타임아웃, 재시도, 연결 실패를 HTTP 관점에서 보기

네트워크는 항상 실패할 수 있다.

### 실패 유형
- DNS 문제
- TCP 연결 실패
- TLS 핸드셰이크 실패
- HTTP 응답 지연
- 서버 내부 에러
- 프록시 타임아웃

### 이 프로젝트에서 체감되는 지점
- 외부 거래소 API
- OAuth2 제공자 호출
- Nginx 뒤 upstream 응답 지연
- 토큰 재발급 시도 중 중복 요청

### 실전 감각
- "응답이 늦다"는 애플리케이션만의 문제가 아닐 수 있다.
- 네트워크 계층, TLS, 프록시, 백엔드, DB 중 어디에서 병목이 나는지 분리해서 봐야 한다.

---

## 17. 운영에서 반드시 구분해야 할 HTTP와 HTTPS 관련 문제 유형

### 1. 경로 문제
- `/api` prefix 누락
- 프록시 rewrite 실수
- frontend 환경변수 URL 오설정

### 2. CORS 문제
- origin 불일치
- `withCredentials` 사용 중인데 서버 CORS 헤더 부족
- `http`와 `https` 혼용

### 3. 쿠키 문제
- `Secure` 누락
- `HttpOnly` 누락
- `SameSite` 정책 미설정
- 도메인/경로 불일치

### 4. OAuth2 문제
- callback URL 불일치
- redirect URL 도메인 불일치
- 프록시 뒤 base URL 인식 실패

### 5. 프록시 문제
- Nginx가 upstream 포트를 잘못 바라봄
- `X-Forwarded-*` 전달 미흡
- WebSocket 업그레이드 헤더 누락

### 6. HTTPS 문제
- 인증서 미설치
- 인증서 갱신 실패
- 혼합 콘텐츠(`https` 페이지에서 `http` 리소스 요청)

---

## 18. 이 프로젝트에서 특히 중요하게 봐야 할 현재 체크포인트

### 체크포인트 1. Refresh Token 쿠키 보안 속성
- 운영 배포 전 `HttpOnly`, `Secure`, 필요 시 `SameSite=None` 여부를 재검토해야 한다.
- 지금처럼 주석 처리된 상태는 학습용/임시 상태로 보고 운영 기준에서 다시 설계해야 한다.

### 체크포인트 2. 프론트와 백엔드의 배치 방식
- 서로 다른 origin으로 두면 CORS와 쿠키 정책이 복잡해진다.
- 가능하면 `https://www.ubot.site` 아래에서 `Nginx -> /api -> backend` 구조로 단일 사이트 운영이 더 단순하다.

### 체크포인트 3. OAuth2 redirect 일관성
- 카카오 설정, 백엔드 `redirect-uri`, 프론트 redirect URL, 실제 도메인이 전부 일치해야 한다.

### 체크포인트 4. WebSocket 보안 스킴
- 운영 페이지가 HTTPS면 WebSocket도 `wss://`를 쓰는 습관이 필요하다.
- 내부 WebSocket을 나중에 직접 제공하게 되면 Nginx 업그레이드 설정까지 함께 봐야 한다.

### 체크포인트 5. 프록시 신뢰 헤더
- 프록시 뒤에서 redirect URL이나 request scheme 판단이 필요하면 forwarded header 설정과 Nginx 전달 헤더가 중요하다.

---

## 19. 우선순위 학습 순서

### 1단계. HTTP 메시지와 상태 코드
- 요청/응답 구조
- 메서드
- 상태 코드
- 헤더

### 2단계. 쿠키, 인증, OAuth2
- `Authorization` 헤더
- `Set-Cookie`
- `HttpOnly`, `Secure`, `SameSite`
- redirect 흐름

### 3단계. CORS와 브라우저 정책
- origin
- preflight
- credentials

### 4단계. HTTPS와 인증서
- TLS
- 인증서
- mixed content

### 5단계. Nginx Reverse Proxy
- 경로 라우팅
- forwarded headers
- WebSocket proxy

### 6단계. 캐시와 운영 문제해결
- 정적 캐시
- 인증 응답 비캐시
- 장애 분류

---

## 20. 이 문서를 읽고 바로 점검해야 할 질문

아래 질문에 스스로 답할 수 있으면, 지금 프로젝트에 필요한 HTTP/HTTPS 핵심은 꽤 잡힌 상태다.

1. Access Token과 Refresh Token은 각각 어디에 저장되고 어떻게 전송되는가?
2. 왜 `withCredentials: true`를 쓰면 CORS 설정이 더 엄격해지는가?
3. 왜 운영에서는 `Secure` 쿠키가 사실상 필수인가?
4. OAuth2 로그인은 왜 일반 API 호출과 다른 방식으로 디버깅해야 하는가?
5. 프론트와 백엔드를 같은 도메인 아래 두면 무엇이 단순해지는가?
6. Nginx가 앞단에 있을 때 애플리케이션이 원래 요청이 HTTPS였다는 사실을 왜 알아야 하는가?
7. 왜 HTTPS 페이지에서 `ws://`는 문제가 되고 `wss://`는 안전한가?
8. 401, 404, 502를 봤을 때 각각 가장 먼저 의심해야 할 계층은 어디인가?

---

## 21. 한 줄 정리

이 프로젝트에서 HTTP/HTTPS를 안다는 것은 단순히 "요청 보내고 응답 받는다"를 아는 것이 아니라, `토큰`, `쿠키`, `OAuth2 redirect`, `CORS`, `Reverse Proxy`, `TLS`, `WebSocket`이 하나의 사용자 흐름 안에서 어떻게 연결되는지 이해하는 것이다.

운영 품질은 코드만으로 결정되지 않고, 이 흐름을 얼마나 정확히 이해했는지로 결정된다.
