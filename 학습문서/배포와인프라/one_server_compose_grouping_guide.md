# 한 서버 Docker Compose 묶음 가이드

## 왜 이 문서가 필요한가
- 이 프로젝트는 학습자가 배포 전 과정을 이해하기 위한 학습형 프로젝트다.
- 그래서 처음부터 서버를 여러 대로 나누는 것보다, 한 대의 서버에서 역할을 분리해 전체 흐름을 먼저 이해하는 편이 더 적합하다.
- 다만 한 서버라고 해서 모든 컨테이너를 한 파일에 무조건 다 넣는 방식이 항상 좋은 것은 아니다.

## 핵심 결론
- 한 서버에서 운영해도 된다.
- 하지만 `서비스 성격`과 `변경 주기`가 다른 것들을 기준 없이 한 덩어리로 묶으면 관리가 빠르게 어려워진다.
- 이 프로젝트에서는 `역할별 Compose 분리 + 공통 네트워크 연결` 방식이 가장 합리적이다.

## 어떤 이론에서 나온 방식인가
### 1. 관심사 분리
- 인프라 설계에서 서로 책임이 다른 구성요소를 나누는 이유는 변경 영향 범위를 줄이기 위해서다.
- 이 이론이 현재 Docker Compose 설계로 이어지면, 데이터 저장소, 애플리케이션, 배포 도구를 서로 다른 묶음으로 나누는 방식이 된다.

### 2. 생명주기 분리
- 어떤 서비스는 거의 항상 떠 있어야 하고, 어떤 서비스는 배포 시점에만 자주 건드린다.
- 이 이론이 현재 기술과 연결되면 `MySQL`, `Redis`, `Kafka` 같은 데이터/메시징 계층은 안정적으로 오래 유지하고, `backend`, `frontend`, `strategy_fastapi`는 배포 때 자주 교체하며, `Jenkins`는 운영 앱과 별도 관리 대상으로 본다.

### 3. 장애 반경 축소
- 하나를 바꿀 때 다른 것까지 같이 흔들리지 않게 하는 것이 운영의 기본 원리다.
- Compose를 역할별로 나누면 `docker compose up -d`를 할 때도 필요한 묶음만 건드릴 수 있다.

## 이 프로젝트에 맞는 추천 묶음
### 1. 데이터 계층
- `mysql`
- `redis`
- `kafka`
- 필요하면 Kafka용 `zookeeper` 또는 KRaft 설정 포함

이 묶음의 의미:
- 애플리케이션이 의존하는 기반 서비스
- 자주 재배포하지 않는 편이 좋음
- 볼륨 관리와 백업 관점이 중요함

추천 파일명:
- `compose.data.yml`

### 2. 애플리케이션 계층
- `nginx`
- `frontend` (Next.js)
- `backend` (Spring Boot)
- `strategy` (FastAPI)

이 묶음의 의미:
- 실제 사용자 요청을 처리하는 서비스
- 코드 변경이 가장 자주 일어나는 영역
- `blue/green`을 나중에 붙일 경우 `backend-blue`, `backend-green`처럼 이 계층 안에서 확장하는 편이 자연스럽다

추천 파일명:
- `compose.app.yml`

### 3. 배포/자동화 계층
- `jenkins`

이 묶음의 의미:
- 운영 트래픽을 직접 처리하지 않음
- 빌드, 이미지 생성, 배포 스크립트 실행의 책임을 가짐
- 나중에 분리 서버로 옮기기 가장 쉬운 대상

추천 파일명:
- `compose.ci.yml`

## 왜 이렇게 나누는 것이 지금 프로젝트에 맞는가
- 현재 프로젝트는 Oracle 인스턴스 한 대에서 전체 흐름을 학습하는 단계다.
- 이때 중요한 것은 "한 대냐 여러 대냐"보다 "어떤 역할이 서로 다른가"를 구분해서 보는 것이다.
- `MySQL/Redis/Kafka`는 데이터와 메시징 기반이고, `backend/frontend/strategy`는 실제 앱이고, `Jenkins`는 배포 도구다.
- 즉 기술 이름으로 묶기보다 `운영 책임`으로 묶어야 이후에도 확장이 쉽다.

## 추천 디렉토리 구조
```text
deploy/
  compose/
    compose.data.yml
    compose.app.yml
    compose.ci.yml
    .env.data
    .env.app
    .env.ci
  nginx/
    nginx.conf
    conf.d/
  jenkins/
    Dockerfile
    casc/
  scripts/
    deploy.sh
    switch.sh
    health-check.sh
```

## 한 파일에 전부 넣지 말자는 이유
한 파일에 모두 넣는 방식은 처음에는 쉬워 보이지만 아래 문제가 생기기 쉽다.

- `frontend`만 다시 배포하고 싶은데 `mysql`, `redis`, `kafka` 설정까지 같이 보게 된다.
- `jenkins`를 수정하다가 운영 앱 정의와 섞여 실수할 가능성이 커진다.
- 장애가 났을 때 어느 계층 문제인지 빠르게 분리해서 보기 어렵다.
- 학습 관점에서도 "데이터 계층", "앱 계층", "배포 계층"이 흐릿해진다.

## 그렇다고 너무 잘게 쪼개지 말아야 하는 이유
반대로 Compose 파일을 서비스별로 전부 나누면 초반 학습에는 오히려 부담이 커진다.

- 파일 수가 너무 많아진다.
- 실행 순서를 이해하기 어려워진다.
- 아직 역할보다 도구 사용법에 정신이 쏠릴 수 있다.

그래서 현재 단계에서는 `3개 묶음` 정도가 가장 균형이 좋다.

## 실제 실행 방식 예시
데이터 계층 먼저:

```bash
docker compose -f deploy/compose/compose.data.yml up -d
```

애플리케이션 계층 실행:

```bash
docker compose -f deploy/compose/compose.app.yml up -d --build
```

Jenkins는 필요할 때만:

```bash
docker compose -f deploy/compose/compose.ci.yml up -d
```

여러 파일을 함께 쓸 수도 있다:

```bash
docker compose \
  -f deploy/compose/compose.data.yml \
  -f deploy/compose/compose.app.yml \
  up -d
```

## 네트워크와 볼륨 원칙
### 네트워크
- `app_net`: nginx, frontend, backend, strategy
- `data_net`: mysql, redis, kafka, backend, strategy
- `ci_net`: jenkins가 필요 시 app/data 쪽에 접근

이론적으로 네트워크 분리는 통신 경계를 명확히 하기 위한 방법이다.
- 현재 기술에서는 컨테이너가 같은 네트워크에 있을 때만 서비스명으로 서로를 찾을 수 있다.
- 그래서 `backend`는 `mysql`, `redis`, `kafka`를 서비스명으로 접근하고, `nginx`는 `frontend`, `backend`를 서비스명으로 프록시하도록 두는 편이 자연스럽다.

### 볼륨
- `mysql_data`
- `redis_data`
- `kafka_data`
- `jenkins_home`

데이터 저장소와 Jenkins는 반드시 named volume으로 분리하는 편이 좋다.

## 지금 서비스들을 어떻게 배치하면 좋은가
### nginx
- 외부 80/443을 받는 진입점
- app 계층

### mysql
- 영속 데이터 저장
- data 계층

### redis
- 캐시, 토큰, 차트 데이터
- data 계층

### kafka
- 이벤트 전달
- data 계층

### frontend
- 사용자 UI
- app 계층

### backend
- 메인 API, OAuth2, JWT, 스케줄러
- app 계층

### strategy_fastapi
- 전략 처리, Redis/Kafka 연동
- app 계층

### jenkins
- 빌드, 이미지 생성, 배포 스크립트 실행
- ci 계층

## Blue/Green까지 생각하면 어떻게 확장되는가
처음:
- `frontend`
- `backend`
- `strategy`

그 다음:
- `frontend`
- `backend-blue`
- `backend-green`
- `strategy`
- `nginx`

즉 Blue/Green은 `app 계층 내부의 배포 방식` 문제이지, 처음부터 전체 인프라 묶음을 다시 갈아엎는 문제는 아니다.

## 현재 단계에서 가장 현실적인 시작 순서
1. `mysql`, `redis`부터 먼저 붙인다.
2. `backend` 단독 연결을 확인한다.
3. `frontend`를 붙이고 `nginx` reverse proxy까지 확인한다.
4. 그 다음 `kafka`와 `strategy_fastapi`를 붙인다.
5. 마지막에 `jenkins`를 붙여 자동화한다.

이 순서를 추천하는 이유:
- 의존성이 적은 것부터 쌓아 올리면 어디서 깨지는지 찾기 쉽다.
- Kafka와 Jenkins는 학습 가치가 크지만 초반 복잡도도 크게 올린다.
- 그래서 첫 목표는 "사용자 요청이 Nginx -> Frontend/Backend로 안정적으로 흐른다"를 만드는 것이 더 좋다.

## 최종 추천
- 한 서버에서 시작해도 된다.
- 하지만 아래처럼 나눠 관리하는 것을 추천한다.

```text
compose.data.yml  -> mysql, redis, kafka
compose.app.yml   -> nginx, frontend, backend, strategy_fastapi
compose.ci.yml    -> jenkins
```

- 이 구조는 학습용으로 충분히 단순하면서도, 나중에 운영 서버와 배포 서버를 물리적으로 분리할 때도 거의 같은 개념을 유지할 수 있다.
