# SSH 서버 운영 명령 퀵 레퍼런스

## 문서 목적
- SSH로 서버에 접속한 뒤 Docker 컨테이너 상태, 로그, 네트워크, 내부 점검을 빠르게 확인할 때 바로 펼쳐보는 용도로 쓴다.
- 긴 설명보다 실전에서 자주 치는 명령을 우선 정리한다.

## 가장 자주 쓰는 핵심 5개
```bash
docker ps
docker compose -f deploy/app/docker-compose.yml logs -f backend
docker exec -it ubot-backend sh
docker inspect ubot-backend
docker network inspect ubot_app_net
```

## 1. 컨테이너 상태 확인
```bash
docker ps
docker ps -a
docker stats
docker inspect 컨테이너명
```

- `docker ps`: 현재 살아 있는 컨테이너만 본다.
- `docker ps -a`: 종료된 컨테이너까지 포함해서 본다.
- `docker stats`: CPU, 메모리, 네트워크 사용량을 본다.
- `docker inspect`: 포트, 네트워크, 볼륨, 환경변수, 라벨을 자세히 본다.

## 2. 로그 확인
```bash
docker logs -f 컨테이너명
docker logs --tail=100 컨테이너명

docker compose -f deploy/app/docker-compose.yml logs -f backend
docker compose -f deploy/app/docker-compose.yml logs -f frontend
docker compose -f deploy/app/docker-compose.yml logs -f strategy_fastapi

docker compose -f deploy/infra/docker-compose.yml logs -f nginx
docker compose -f deploy/infra/docker-compose.yml logs -f mysql
docker compose -f deploy/infra/docker-compose.yml logs -f kafka
```

- `-f`: 실시간으로 계속 따라간다.
- `--tail=100`: 최근 100줄만 먼저 본다.
- 가능하면 개별 `docker logs`보다 `docker compose logs`를 우선 쓰는 편이 서비스 단위로 보기 편하다.

## 3. 컨테이너 안으로 들어가기
```bash
docker exec -it 컨테이너명 sh
docker exec -it 컨테이너명 bash

docker exec -it ubot-backend date
docker exec -it ubot-backend sh -c 'echo $TZ'
docker exec -it ubot-mysql mysql -u ubot -p
```

- Alpine 계열 이미지는 보통 `sh`를 쓴다.
- Ubuntu, Debian 계열은 `bash`가 있을 수 있다.
- 시간대, 환경변수, DB 접속 상태를 볼 때 자주 쓴다.

## 4. 재시작, 중지, 재기동
```bash
docker restart 컨테이너명
docker stop 컨테이너명
docker start 컨테이너명
docker rm -f 컨테이너명

docker compose -f deploy/app/docker-compose.yml restart backend
docker compose -f deploy/infra/docker-compose.yml up -d
docker compose -f deploy/app/docker-compose.yml up -d --build
```

- 운영 중에는 개별 컨테이너보다 `docker compose ... restart 서비스명`이 더 자연스럽다.
- 코드 변경이 있으면 `up -d --build`로 다시 올린다.

## 5. 이미지, 볼륨, 네트워크 확인
```bash
docker images
docker volume ls
docker volume inspect 볼륨명
docker network ls
docker network inspect ubot_app_net
```

- 볼륨이 꼬였는지, 어떤 네트워크에 붙어 있는지 확인할 때 쓴다.
- 상태가 이상한데 코드가 멀쩡해 보이면 볼륨과 네트워크를 먼저 의심한다.

## 6. Compose 기준으로 서비스 보기
```bash
docker compose -f deploy/app/docker-compose.yml ps
docker compose -f deploy/app/docker-compose.yml config --services

docker compose -f deploy/infra/docker-compose.yml ps
docker compose -f deploy/infra/docker-compose.yml config --services
```

- `ps`: 지금 떠 있는 서비스 상태를 본다.
- `config --services`: compose 파일 안에 정의된 서비스 이름만 뽑아 본다.

## 7. 리눅스에서 같이 자주 보는 명령
```bash
ss -ltnp
sudo lsof -i :80
free -h
df -h
top
htop
journalctl -u nginx
```

- 포트 충돌, 메모리 부족, 디스크 부족, 시스템 서비스 상태를 확인할 때 쓴다.
- Docker 문제가 아니라 호스트 OS 쪽 문제일 수도 있어서 같이 본다.

## 8. 지금 프로젝트 기준으로 특히 자주 쓰는 명령
```bash
docker ps

docker compose -f deploy/app/docker-compose.yml logs -f backend
docker compose -f deploy/app/docker-compose.yml logs -f frontend
docker compose -f deploy/infra/docker-compose.yml logs -f nginx

docker network inspect ubot_app_net

docker exec -it ubot-backend date
docker exec -it ubot-backend sh -c 'echo $TZ'
docker exec -it ubot-mysql mysql -u ubot -p
```

## 9. 운영 중 문제 생겼을 때 보는 순서
1. `docker ps`
2. `docker compose ... logs --tail=100 서비스명`
3. `docker inspect 컨테이너명`
4. `docker network inspect 네트워크명`
5. 필요하면 `docker exec -it ... sh`

## 10. 한 줄 정리
- 가장 많이 쓰는 축은 `docker ps`, `docker compose logs`, `docker exec`, `docker inspect`, `docker network inspect` 다섯 개다.
- 운영 중 이상 징후가 보이면 로그만 보지 말고 네트워크, 볼륨, 시간대까지 함께 확인하는 습관이 중요하다.
