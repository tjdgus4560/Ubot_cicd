# Ubot CI/CD 학습 및 배포 진행 로드맵

## 문서 목적
- 이 문서는 `오라클 클라우드 VM 접속 성공` 상태에서 시작해 `수동 배포`, `자동 배포`, `Blue/Green 무중단 배포`까지 가기 위해 공부하고 진행해야 할 내용을 순서대로 정리한 문서다.
- 목표는 "한 번에 다 이해"가 아니라, `가장 작은 성공 배포`를 먼저 만들고 그 위에 자동화와 무중단 배포를 점진적으로 붙이는 것이다.

## 현재 출발점
- 완료:
  - 오라클 클라우드 인스턴스 생성
  - 보안키 기반 SSH 접속 성공
- 앞으로 해야 할 큰 흐름:
  1. 서버 기초 세팅
  2. Docker 기반 수동 배포 성공
  3. 도메인, SSL, Nginx Reverse Proxy 연결
  4. Jenkins로 자동 배포 구성
  5. Blue/Green 무중단 배포 적용

## 먼저 꼭 기억할 원칙
1. `자동 배포`보다 먼저 `수동 배포`를 완전히 성공시킨다.
2. `무중단 배포`보다 먼저 `정상 배포 + 로그 확인 + 롤백`이 가능해야 한다.
3. DB, Redis, Kafka 같은 인프라 포트는 외부에 함부로 열지 않는다.
4. 운영 비밀값은 `application.yml`이나 코드에 직접 박지 않고 환경변수나 별도 설정으로 관리한다.
5. 배포는 기능 개발과 다르게 "실패했을 때 어떻게 되돌릴지"까지 준비해야 한다.

---

## 1. 전체 아키텍처 먼저 정리

### 공부할 것
- 웹 서비스 요청 흐름
  - 사용자 -> 도메인 -> Nginx -> Spring Boot / FastAPI / Frontend
- Reverse Proxy 개념
- 내부 포트와 외부 공개 포트의 차이
- 운영 환경과 로컬 환경 분리 개념

### 해야 할 일
- 아래 내용을 표로 정리한다.
  - 서비스 목록
  - 각 서비스 역할
  - 사용할 포트
  - 외부 공개 여부
  - 데이터 저장 위치
  - 환경변수 목록
- 최소한 다음 구조를 문서화한다.
  - `frontend`
  - `spring boot backend`
  - `fastapi`
  - `mysql`
  - `redis`
  - `kafka`
  - `nginx`
  - `jenkins`

### 예시 포트 구조
| 서비스 | 내부 포트 | 외부 공개 여부 | 비고 |
|---|---:|---|---|
| Nginx | 80, 443 | 공개 | 유일한 공개 진입점 권장 |
| Spring Boot | 8080 | 비공개 | Nginx가 프록시 |
| FastAPI | 8000 | 비공개 | Nginx가 프록시 |
| Frontend | 3000 또는 정적 파일 | 보통 비공개 | Nginx 뒤에 둠 |
| MySQL | 3306 | 비공개 | 내부 통신만 권장 |
| Redis | 6379 | 비공개 | 내부 통신만 권장 |
| Kafka | 9092 | 비공개 | 내부 통신만 권장 |
| Jenkins | 8081 등 | 제한 공개 또는 비공개 | IP 제한 권장 |

### 이 단계의 산출물
- 아키텍처 다이어그램 1장
- 포트/도메인/환경변수 정리표 1개

---

## 2. 오라클 클라우드 인프라 기초

### 공부할 것
- VCN
- Subnet
- Security List / Ingress / Egress
- 공인 IP와 사설 IP 차이
- 리눅스 서버 기본 관리

### 해야 할 일
- 인스턴스 OS 업데이트
- 타임존 확인
- 디스크 용량 확인
- 메모리 확인
- 방화벽 규칙 최소화

### 점검 체크리스트
- SSH 접속 가능
- `22` 포트만 우선 열려 있는지 확인
- 필요 시 `80`, `443`만 추가 오픈
- MySQL, Redis, Kafka 포트를 외부에 열지 않았는지 확인
- 인스턴스 스펙이 Kafka, Jenkins, DB까지 감당 가능한지 확인

### 공부하면서 익혀야 할 리눅스 명령어
- `pwd`
- `ls -al`
- `cd`
- `mkdir`
- `cp`
- `mv`
- `rm` 사용 주의
- `chmod`
- `chown`
- `ps`
- `top` 또는 `htop`
- `df -h`
- `free -m`
- `systemctl`
- `journalctl`
- `tail -f`
- `curl`

### 이 단계의 산출물
- 서버 기본 세팅 완료
- 방화벽 정책 정리
- 서버 스펙 기록

---

## 3. Docker를 가장 먼저 익히기

### 왜 먼저 공부해야 하는가
- 지금 문서에 있는 대부분의 배포 작업은 Docker를 중심으로 연결된다.
- Jenkins, Nginx, Spring Boot, FastAPI, MySQL, Redis, Kafka까지 모두 컨테이너 기반으로 묶는 경우가 많다.

### 공부할 것
- 이미지와 컨테이너 차이
- Dockerfile
- Volume
- Network
- Environment Variables
- `docker build`
- `docker run`
- `docker logs`
- `docker exec`
- `docker compose`

### 직접 해볼 것
1. 간단한 Nginx 컨테이너 하나 띄우기
2. Spring Boot 앱을 Dockerfile로 빌드해보기
3. FastAPI 앱을 Dockerfile로 빌드해보기
4. 컨테이너 로그 보기
5. 볼륨 연결해보기
6. 사용자 정의 네트워크 만들기

### 꼭 알아야 할 개념
- 컨테이너를 지우면 데이터가 날아갈 수 있다.
- 그래서 MySQL 같은 것은 볼륨이 매우 중요하다.
- 컨테이너 간 통신은 IP보다 `서비스명`으로 붙이는 것이 일반적이다.
- 운영에서는 `restart` 정책과 `healthcheck`가 중요하다.

### 이 단계의 산출물
- Backend Dockerfile
- Frontend Dockerfile
- FastAPI Dockerfile
- `docker-compose.prod.yml` 초안

---

## 4. 애플리케이션 배포 준비

## 4-1. Spring Boot

### 공부할 것
- `application.yml`
- `application-prod.yml`
- 환경변수 주입
- 프로파일 분리
- 헬스체크 엔드포인트
- 로그 레벨 설정

### 해야 할 일
- 운영용 설정을 분리한다.
- 비밀번호, 토큰, 접속 정보는 환경변수로 빼낸다.
- 배포 후 살아있는지 확인할 수 있도록 헬스체크 엔드포인트를 준비한다.
- DB 연결 정보, Redis 정보, Kafka 정보가 운영 기준으로 정리되어야 한다.

### 체크포인트
- `prod` 프로파일로 실행 가능
- DB 연결 확인 가능
- Redis 연결 확인 가능
- Kafka 연결 확인 가능
- `/actuator/health` 또는 별도 헬스체크 API 제공

## 4-2. FastAPI

### 공부할 것
- `requirements.txt`
- `uvicorn` 또는 `gunicorn` 실행 방식
- 환경변수 관리
- health check API

### 해야 할 일
- `requirements.txt` 정리
- Dockerfile 작성
- 운영용 환경변수 구조 정리
- `/health` 같은 간단한 상태 확인 API 준비

### 체크포인트
- 컨테이너에서 정상 기동
- 외부 의존성 연결 성공
- 로그 확인 가능

## 4-3. Frontend

### 공부할 것
- 빌드 산출물 경로
- 정적 파일 배포 방식
- Nginx 정적 서빙
- API 경로 프록시 처리

### 해야 할 일
- 운영 빌드 확인
- Dockerfile 작성
- 환경별 API URL 전략 정리

### 이 단계의 산출물
- 서비스별 Dockerfile
- 운영 환경변수 목록
- 앱별 헬스체크 경로 목록

---

## 5. docker custom network와 compose 작성

### 공부할 것
- `docker network`
- `docker compose` 서비스 정의
- `depends_on`
- `healthcheck`
- volume과 bind mount 차이

### 해야 할 일
- 서비스들을 한 네트워크로 묶는다.
- 서비스명으로 서로 접근하도록 구성한다.
- MySQL 데이터는 named volume으로 분리한다.
- 로그와 업로드 파일 경로가 있다면 볼륨 설계를 한다.

### compose에 들어가야 할 것
- `spring`
- `fastapi`
- `frontend`
- `mysql`
- `redis`
- `kafka`
- `nginx`

### compose 작성 시 주의점
- `depends_on`만 있다고 서비스가 "사용 가능 상태"가 보장되지는 않는다.
- 꼭 `healthcheck`를 고려한다.
- 포트 충돌을 미리 점검한다.
- `.env` 파일 또는 배포용 변수 주입 방식을 정한다.

### 이 단계의 목표
- Jenkins 없이도 한 번의 `docker compose up -d`로 서비스들이 뜬다.

---

## 6. 수동 배포를 먼저 완성하기

### 가장 중요한 단계
- 이 단계가 안 되면 Jenkins를 붙여도 문제가 자동화될 뿐 해결되지 않는다.

### 수동 배포 순서 예시
1. 서버에서 코드 가져오기
2. 환경변수 준비
3. Docker 이미지 빌드
4. `docker compose up -d`
5. 로그 확인
6. 헬스체크 확인
7. 웹 접속 및 API 테스트

### 꼭 해봐야 하는 것
- 첫 배포
- 수정 후 재배포
- 컨테이너 재시작
- 로그로 에러 찾기
- 이전 버전으로 롤백

### 수동 배포 시 확인할 것
- 배포 후 서비스가 실제로 응답하는가
- DB 연결은 정상인가
- Redis, Kafka 연결은 정상인가
- 파일 업로드/다운로드가 있다면 잘 동작하는가
- 재배포 시 데이터가 유지되는가

### 이 단계의 산출물
- 수동 배포 절차 문서
- 배포 체크리스트
- 롤백 체크리스트

---

## 7. Git과 브랜치, 버전 관리

### 공부할 것
- main / develop / feature 브랜치 전략
- 태그 기반 배포
- 커밋과 배포 버전 연결
- 롤백할 때 어느 커밋으로 돌아갈지 추적하는 방법

### 해야 할 일
- 배포 가능한 브랜치를 정한다.
- 배포 시 이미지 태그 규칙을 정한다.
  - 예: `latest`만 쓰지 말고 `build-number`, `commit-sha`, `date tag` 등을 같이 사용
- 운영 배포 이력을 기록한다.

### 꼭 필요한 이유
- Blue/Green이나 롤백은 "어느 버전이 현재 운영 중인지"를 정확히 알아야 안전하다.

---

## 8. 도메인, SSL, Nginx Reverse Proxy

### 공부할 것
- DNS
- A 레코드
- HTTP와 HTTPS 차이
- SSL 인증서 발급
- Reverse Proxy
- Nginx 설정 구조

### 해야 할 일
- 도메인을 공인 IP에 연결한다.
- Nginx를 앞단에 둔다.
- `80 -> 443` 리다이렉트를 구성한다.
- SSL 인증서를 발급하고 자동 갱신 방법을 확인한다.
- 경로별 프록시 정책을 정한다.

### 예시 라우팅
- `/` -> Frontend
- `/api` -> Spring Boot
- `/fastapi` 또는 `/py` -> FastAPI

### Nginx에서 자주 놓치는 것
- 큰 파일 업로드 제한
- WebSocket 사용 여부
- CORS 처리 위치
- 정적 파일 캐시 전략
- Nginx reload 시 서비스 영향

### 이 단계의 산출물
- 운영 도메인 연결 완료
- HTTPS 적용 완료
- Nginx 설정 파일 초안

---

## 9. MySQL 운영 시 알아야 할 것

### 공부할 것
- MySQL 볼륨 관리
- 백업과 복구
- 스키마 변경 전략
- 커넥션 설정
- 문자셋 / 타임존

### 해야 할 일
- 데이터 저장용 volume 구성
- 초기 스키마 적용 방법 정리
- 백업 방법 정리
- 비밀번호와 사용자 권한 정리

### 특히 중요
- Blue/Green 전환 시 DB 스키마가 구버전/신버전 둘 다 호환되는지 확인해야 한다.
- 스키마 변경이 깨지면 앱만 롤백해도 복구가 안 되는 경우가 있다.

### 권장 학습 키워드
- Flyway
- Liquibase
- migration
- rollback 전략

---

## 10. Redis 운영 시 알아야 할 것

### 공부할 것
- Redis를 캐시로 쓰는지 세션으로 쓰는지
- persistence 필요 여부
- eviction 정책
- 메모리 사용량

### 해야 할 일
- Redis 역할을 명확히 한다.
- 운영에서 데이터 유지가 필요한지 결정한다.
- 비밀번호 또는 네트워크 제한을 검토한다.

### 주의점
- 외부 포트를 열어두면 위험하다.
- 캐시 장애 시 서비스가 어떻게 동작해야 하는지도 생각해야 한다.

---

## 11. Kafka 운영 시 알아야 할 것

### 공부할 것
- 브로커
- 토픽
- 프로듀서
- 컨슈머
- consumer group
- offset
- 단일 노드 Kafka의 한계

### 해야 할 일
- 현재 프로젝트에서 Kafka가 정말 필요한지 확인한다.
- 토픽 목록과 용도를 정리한다.
- 컨슈머가 여러 개 실행될 때 중복 처리 문제가 없는지 확인한다.

### 특히 중요
- Blue/Green 배포 시 구버전과 신버전 컨슈머가 동시에 떠 있으면 중복 처리 문제가 생길 수 있다.
- 배치, 스케줄러, 메시지 소비 로직은 무중단 배포와 충돌하기 쉽다.

### 현실적인 체크
- 서버 메모리가 충분한지 먼저 확인한다.
- 단일 서버에서 Kafka까지 같이 운영하면 리소스 압박이 크다.

---

## 12. Jenkins와 자동 배포

### 공부할 것
- CI와 CD 차이
- Jenkins Pipeline
- Jenkins Credentials
- Webhook
- 빌드, 테스트, 배포 단계 분리

### Jenkins에서 보통 하는 일
1. Git에서 코드 가져오기
2. 테스트 실행
3. 빌드 수행
4. Docker 이미지 빌드
5. 이미지 태그 생성
6. 이미지 저장소 push
7. 운영 서버 배포
8. 헬스체크
9. 실패 시 중단 또는 롤백

### 해야 할 일
- Jenkins 설치
- 필수 플러그인 설치
- Credentials 등록
- 파이프라인 스크립트 작성

### 플러그인/구성에서 자주 필요한 것
- Git plugin
- Pipeline
- Credentials Binding
- SSH Agent 또는 SSH 관련 플러그인
- Docker 관련 플러그인

### 꼭 알아야 할 점
- Jenkins가 빌드만 하고 배포는 서버에서 pull 받는 방식인지
- Jenkins가 직접 이미지를 build/push 하고 서버는 이미지를 pull 하는 방식인지
- 어떤 배포 방식을 택할지 먼저 정해야 한다

### 추천 방식
- `Jenkins -> Docker Image Build -> Registry Push -> Server Pull -> Compose Up`
- 이 방식이 버전 관리와 롤백이 더 수월한 편이다.

### 이 단계의 산출물
- Jenkinsfile
- 자동 배포 성공
- 실패 로그 확인 가능

---

## 13. Blue/Green 무중단 배포

### 공부할 것
- Blue/Green 배포 개념
- Rolling 배포와의 차이
- 트래픽 전환 방식
- 헬스체크 기반 전환
- 롤백 전략

### 개념 정리
- `blue`: 현재 운영 중인 버전
- `green`: 새로 배포하는 버전
- 새 버전을 먼저 띄운 뒤 건강 상태를 확인하고, Nginx가 바라보는 대상을 바꾼다.

### 해야 할 일
1. blue와 green 두 개의 앱 세트를 띄울 수 있게 구성한다.
2. Nginx upstream 또는 proxy target 전환 구조를 만든다.
3. 새 버전 헬스체크 성공 시 전환한다.
4. 문제 발생 시 즉시 기존 버전으로 되돌린다.

### 구현 전에 꼭 생각할 것
- DB 스키마가 구버전/신버전 모두 호환되는가
- Kafka consumer 중복 실행 문제가 없는가
- 스케줄러가 두 번 돌지 않는가
- 세션/캐시 일관성이 깨지지 않는가

### Blue/Green의 핵심 성공 조건
- "새 버전이 떴다"가 아니라 "전환 전 검증이 가능하다"가 핵심이다.
- 롤백이 빨라야 의미가 있다.

### 이 단계의 산출물
- Blue/Green 전환 스크립트 또는 파이프라인
- 헬스체크 기반 전환 로직
- 롤백 절차 문서

---

## 14. Multi Stage Build

### 공부할 것
- Docker Multi-stage build 개념
- 빌드 이미지와 실행 이미지 분리

### 왜 필요한가
- 이미지 크기 축소
- 보안 개선
- 빌드 도구와 런타임 분리

### 적용 대상
- Spring Boot
- Frontend
- 경우에 따라 FastAPI도 가능

### 기대 효과
- 배포 속도 개선
- 불필요한 파일 제거
- 운영 이미지 경량화

---

## 15. 로그, 모니터링, 장애 대응

### 공부할 것
- 컨테이너 로그 확인
- Nginx 로그 확인
- Spring Boot 로그 확인
- FastAPI 로그 확인
- 장애 시 어디서부터 확인할지 순서 정하기

### 최소 체크포인트
- `docker logs`
- Nginx access/error log
- 앱 헬스체크 API
- 서버 CPU/메모리/디스크 사용량

### 운영에서 꼭 필요한 습관
- 배포 직후 로그를 반드시 확인한다.
- "컨테이너가 떴다"와 "서비스가 정상이다"는 다르다.
- 장애 시 최근 배포 여부를 가장 먼저 의심한다.

---

## 16. 보안 관점에서 꼭 챙길 것

### 해야 할 일
- SSH 키 권한 관리
- root 직접 접속 최소화
- 외부 오픈 포트 최소화
- DB/Redis/Kafka 외부 비공개
- 비밀번호 및 토큰 하드코딩 금지
- Jenkins 관리자 계정 보호
- HTTPS 적용

### 추가로 공부하면 좋은 것
- Fail2ban
- 방화벽 정책
- 비밀값 관리 방식
- 접근 로그 점검

---

## 17. 실제 추천 진행 순서

### 1단계: 기초 정리
- 서버 스펙 확인
- 포트 계획
- 환경변수 목록 작성
- 아키텍처 그림 작성

### 2단계: 컨테이너화
- Spring Boot Dockerfile
- FastAPI Dockerfile
- Frontend Dockerfile
- 운영용 설정 정리

### 3단계: 수동 배포 성공
- `docker compose` 작성
- MySQL/Redis/Kafka 연결
- 로그 확인
- 헬스체크 확인

### 4단계: 외부 연결
- 도메인 연결
- SSL 인증서 발급
- Nginx reverse proxy 구성

### 5단계: 자동화
- Jenkins 설치
- Jenkinsfile 작성
- 자동 빌드/배포 연결

### 6단계: 무중단 배포
- blue/green 구조 도입
- 전환 및 롤백 자동화

---

## 18. 실습 체크리스트

### 배포 전
- [ ] 서비스 구조도 작성
- [ ] 포트표 작성
- [ ] 환경변수 목록 작성
- [ ] 운영 설정 분리
- [ ] Dockerfile 작성
- [ ] compose 초안 작성

### 수동 배포
- [ ] 컨테이너 기동 성공
- [ ] DB 연결 성공
- [ ] Redis 연결 성공
- [ ] Kafka 연결 성공
- [ ] 헬스체크 성공
- [ ] 브라우저/API 테스트 성공
- [ ] 로그 확인 완료

### 외부 서비스 오픈
- [ ] 도메인 연결
- [ ] HTTPS 적용
- [ ] Nginx Reverse Proxy 성공

### 자동 배포
- [ ] Jenkins 설치
- [ ] Jenkins Credentials 등록
- [ ] Pipeline 작성
- [ ] 자동 배포 성공

### 무중단 배포
- [ ] blue/green 구성 완료
- [ ] 전환 테스트 성공
- [ ] 롤백 테스트 성공

---

## 19. 지금 바로 해야 할 우선순위
1. 서버 리소스와 포트 계획을 문서로 정리한다.
2. Spring Boot, FastAPI, Frontend의 Dockerfile을 만든다.
3. `docker-compose.prod.yml`로 수동 배포를 먼저 성공시킨다.
4. Nginx와 SSL을 붙여 외부 접속을 완성한다.
5. Jenkins를 붙여 자동 배포를 만든다.
6. 마지막으로 Blue/Green과 롤백을 붙인다.

---

## 20. 가장 많이 막히는 포인트
- Docker는 떴는데 앱이 내부 의존성(MySQL/Redis/Kafka)에 연결되지 않음
- 운영용 `application.yml` 설정이 로컬 기준으로 남아 있음
- 포트는 열었지만 오라클 클라우드 보안 규칙이 막고 있음
- SSL은 붙였는데 Nginx 프록시 설정이 잘못됨
- Jenkins는 돌았는데 실제 운영 서버 반영이 안 됨
- Blue/Green 전환은 됐지만 DB 마이그레이션 때문에 장애 발생
- Kafka consumer가 중복 실행되어 이벤트가 두 번 처리됨

---

## 21. 추천 학습 순서 요약
1. 리눅스 서버 기본
2. Docker / Docker Compose
3. Spring Boot / FastAPI 운영 설정
4. 수동 배포와 롤백
5. Nginx / 도메인 / SSL
6. Jenkins Pipeline
7. Blue/Green 무중단 배포
8. DB migration, 로그, 모니터링, 보안

---

## 22. 한 줄 결론
- 처음 배포라면 `수동 배포 성공 -> 안정화 -> 자동화 -> 무중단 배포` 순서로 가는 것이 가장 안전하다.
- 지금 가장 먼저 해야 할 것은 Jenkins가 아니라 `Docker 기반 수동 배포 완성`이다.
