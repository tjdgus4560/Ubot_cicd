# 인프라 격리 폴더

이 폴더는 기존에 작성되어 있던 인프라 관련 파일을 프로젝트 최상단에서 별도로 분리해 둔 위치입니다.

현재 이동한 파일
- `docker-compose.yml`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `strategy_fastapi/Dockerfile`

이동 기준
- 애플리케이션 소스코드와 분리 가능한 배포/컨테이너 정의 파일만 우선 격리
- `application-prod.yml` 같은 런타임 설정 파일은 애플리케이션 내부 설정이므로 원래 위치 유지

참고
- 이 이동은 파일 정리를 위한 격리이며, 실제 배포 재구성 시에는 경로 기준을 다시 맞춰야 합니다.
