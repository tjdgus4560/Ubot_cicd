# 리눅스 인프라 명령어 정리

## 빠른 요약표

| 영역 | 자주 쓰는 명령어 | 언제 바로 쓰나 |
|---|---|---|
| 현재 위치/파일 확인 | `pwd`, `ls -al`, `tree` | 지금 어디서 작업 중인지, 폴더 구조가 어떻게 생겼는지 볼 때 |
| 디렉토리 이동/생성 | `cd`, `mkdir -p` | 배포용 작업 폴더를 만들고 이동할 때 |
| 파일 복사/이동/삭제 | `cp`, `mv`, `rm` | 설정 파일 복사, 이름 변경, 불필요한 파일 정리 시 |
| 파일 내용 확인 | `cat`, `less`, `head`, `tail -f`, `grep` | 설정 파일 읽기, 긴 로그 보기, 에러 문자열 찾기 |
| OS/시스템 정보 | `uname -a`, `cat /etc/os-release` | 서버 종류와 배포판 확인 시 |
| 디스크/메모리 확인 | `df -h`, `du -sh`, `free -m`, `top` | 용량 부족, 메모리 부족, 서버 상태 점검 시 |
| 시간 설정 확인 | `timedatectl` | 로그 시간, JWT 만료 시간, 스케줄 시간 확인 시 |
| 프로세스 확인 | `ps -ef`, `kill` | 어떤 프로세스가 떠 있는지, 특정 프로세스를 종료할 때 |
| 서비스 관리 | `systemctl`, `journalctl` | Docker, Nginx 같은 서비스 상태/로그 확인 시 |
| 네트워크/IP 확인 | `ip addr`, `ping`, `nslookup`, `traceroute` | IP 확인, 외부 연결, DNS 해석 문제 확인 시 |
| 포트 확인 | `ss -tulpn`, `curl` | 실제 열려 있는 포트와 서비스 응답 여부 확인 시 |
| 권한 관리 | `whoami`, `id`, `chmod`, `chown`, `sudo` | 현재 사용자 확인, 권한 수정, 관리자 명령 실행 시 |
| 패키지 설치 | `dnf`, `yum`, `apt`, `which` | Git, Docker 등 필수 도구 설치 전후 확인 시 |
| 압축/전송 | `tar`, `scp`, `rsync` | 파일 백업, 서버 전송, 동기화 시 |
| Docker 기본 운영 | `docker ps`, `docker logs`, `docker exec`, `docker images` | 컨테이너 상태 확인, 로그 확인, 컨테이너 내부 점검 시 |
| Docker Compose 운영 | `docker compose up -d`, `down`, `ps`, `logs -f` | 여러 서비스 묶음 실행/중지/상태 점검 시 |

## 문서 목적
- 인프라 설정, 패키지 설치, 서버 점검, 배포, 운영 관리 시 자주 쓰는 리눅스 명령어를 한 곳에 정리한다.
- 단순 암기용 목록이 아니라, `무슨 상황에서 왜 쓰는지`, `어떤 결과를 보면 되는지`, `무엇을 조심해야 하는지`를 함께 이해하는 것을 목표로 한다.

## 먼저 이해할 관점
- 리눅스 명령어는 "서버 상태를 본다", "파일을 다룬다", "프로세스를 관리한다", "네트워크를 점검한다", "패키지를 설치한다"처럼 역할별로 기억하면 훨씬 쉽다.
- 인프라 작업에서 중요한 것은 명령어를 많이 아는 것보다, 지금 문제가 `파일`, `권한`, `프로세스`, `서비스`, `포트`, `로그`, `패키지` 중 어디에 있는지 분류하는 것이다.

---

## 1. 현재 위치와 파일 구조 확인

### `pwd`
- 기능: 현재 내가 있는 디렉토리 경로를 출력한다.
- 언제 쓰나: 지금 작업 위치가 어디인지 확인할 때
- 예시:
```bash
pwd
```

### `ls`
- 기능: 현재 디렉토리의 파일과 폴더 목록을 본다.
- 자주 쓰는 옵션:
  - `ls -al`: 숨김 파일 포함, 자세히 보기
  - `ls -lh`: 사람이 읽기 쉬운 용량 단위로 보기
- 예시:
```bash
ls -al
ls -lh
```

### `cd`
- 기능: 디렉토리를 이동한다.
- 예시:
```bash
cd /home/opc
cd ~/deploy/ubot
cd ..
```

### `tree`
- 기능: 폴더 구조를 트리 형태로 본다.
- 언제 쓰나: 프로젝트 구조를 빠르게 파악할 때
- 예시:
```bash
tree
tree -L 2
```
- 주의: 기본 설치가 안 되어 있을 수 있다.

---

## 2. 파일과 디렉토리 생성/이동/복사

### `mkdir`
- 기능: 디렉토리를 생성한다.
- 자주 쓰는 옵션:
  - `mkdir -p`: 중간 경로까지 함께 생성
- 예시:
```bash
mkdir -p ~/deploy/ubot
```

### `touch`
- 기능: 빈 파일을 만들거나, 파일의 수정 시간을 갱신한다.
- 예시:
```bash
touch .env
touch deploy.log
```

### `cp`
- 기능: 파일 또는 디렉토리를 복사한다.
- 자주 쓰는 옵션:
  - `cp -r`: 디렉토리 재귀 복사
- 예시:
```bash
cp .env.example .env
cp -r backend backup_backend
```

### `mv`
- 기능: 파일/폴더를 이동하거나 이름을 바꾼다.
- 예시:
```bash
mv docker-compose.yml docker-compose.prod.yml
mv old.log archive.log
```

### `rm`
- 기능: 파일/폴더를 삭제한다.
- 자주 쓰는 옵션:
  - `rm -f`: 강제 삭제
  - `rm -r`: 폴더 재귀 삭제
- 예시:
```bash
rm test.txt
rm -rf temp_dir
```
- 주의: `rm -rf`는 매우 위험하다. 경로를 반드시 다시 확인하고 사용한다.

---

## 3. 파일 내용 확인

### `cat`
- 기능: 파일 전체 내용을 출력한다.
- 예시:
```bash
cat /etc/os-release
cat .env
```
- 언제 쓰나: 짧은 설정 파일을 빠르게 볼 때

### `less`
- 기능: 긴 파일을 페이지 단위로 읽는다.
- 예시:
```bash
less /var/log/messages
less application-prod.yml
```
- 장점: 긴 로그나 설정 파일을 읽기 편하다.

### `head`
- 기능: 파일의 앞부분만 본다.
- 예시:
```bash
head -20 docker-compose.prod.yml
```

### `tail`
- 기능: 파일의 뒷부분만 본다.
- 자주 쓰는 옵션:
  - `tail -f`: 파일 끝을 계속 따라가며 본다
- 예시:
```bash
tail -50 app.log
tail -f app.log
```
- 언제 쓰나: 실시간 로그 확인

### `grep`
- 기능: 텍스트에서 특정 문자열을 찾는다.
- 예시:
```bash
grep "ERROR" app.log
grep -i "docker" install.log
```

### `find`
- 기능: 파일 이름이나 조건으로 파일을 찾는다.
- 예시:
```bash
find . -name "Dockerfile"
find /var/log -name "*.log"
```

---

## 4. 시스템 정보와 리소스 확인

### `uname -a`
- 기능: 커널과 시스템 정보를 확인한다.
- 예시:
```bash
uname -a
```

### `cat /etc/os-release`
- 기능: 배포판 종류를 확인한다.
- 예시:
```bash
cat /etc/os-release
```
- 언제 쓰나: Oracle Linux인지 Ubuntu인지 확인해서 패키지 설치 명령을 결정할 때

### `df -h`
- 기능: 디스크 사용량을 확인한다.
- 예시:
```bash
df -h
```
- 볼 포인트: `/` 파티션 여유 용량이 충분한지

### `du -sh`
- 기능: 특정 디렉토리가 얼마나 용량을 차지하는지 본다.
- 예시:
```bash
du -sh .
du -sh /var/lib/docker
```

### `free -m`
- 기능: 메모리 사용량을 MB 단위로 확인한다.
- 예시:
```bash
free -m
```

### `top`
- 기능: 실시간 CPU/메모리/프로세스 상태를 확인한다.
- 예시:
```bash
top
```

### `htop`
- 기능: `top`보다 보기 쉬운 인터랙티브 모니터링 도구
- 예시:
```bash
htop
```
- 주의: 설치가 안 되어 있을 수 있다.

### `timedatectl`
- 기능: 시간대와 시간 동기화 상태를 확인한다.
- 예시:
```bash
timedatectl
```
- 왜 중요하나: 로그 시간, JWT 만료 시간, 스케줄링 동작에 직접 영향이 있다.

---

## 5. 프로세스와 서비스 관리

### `ps -ef`
- 기능: 현재 실행 중인 프로세스를 본다.
- 예시:
```bash
ps -ef
ps -ef | grep java
ps -ef | grep docker
```

### `kill`
- 기능: 프로세스를 종료한다.
- 예시:
```bash
kill 1234
kill -9 1234
```
- 주의: `-9`는 강제 종료라서 마지막 수단으로 쓴다.

### `systemctl`
- 기능: systemd 기반 서비스 관리
- 자주 쓰는 형태:
```bash
systemctl status docker
systemctl start docker
systemctl stop docker
systemctl restart docker
systemctl enable docker
systemctl disable docker
```

### `journalctl`
- 기능: systemd 서비스 로그 확인
- 예시:
```bash
journalctl -u docker
journalctl -u docker -f
```
- 언제 쓰나: 서비스가 안 뜨는 원인을 볼 때

---

## 6. 네트워크와 포트 확인

### `ip addr`
- 기능: 네트워크 인터페이스와 IP 정보를 본다.
- 예시:
```bash
ip addr
```

### `ss -tulpn`
- 기능: 현재 열려 있는 포트와 연결된 프로세스를 본다.
- 예시:
```bash
ss -tulpn
sudo ss -tulpn
```
- 왜 중요하나:
  - 서버 안에서 실제 어떤 서비스가 어떤 포트를 점유하는지 확인할 수 있다.
  - 클라우드 보안 규칙과 서버 내부 실행 상태를 함께 봐야 한다.

### `curl`
- 기능: HTTP 요청을 직접 보내본다.
- 예시:
```bash
curl http://localhost:8080/actuator/health
curl -I https://example.com
```
- 언제 쓰나: 서버 내부에서 서비스 응답을 확인할 때

### `ping`
- 기능: 네트워크 연결 가능 여부를 확인한다.
- 예시:
```bash
ping 8.8.8.8
ping google.com
```
- 주의: 일부 환경에서는 막혀 있을 수 있다.

### `nslookup`
- 기능: 도메인이 어떤 IP로 해석되는지 확인한다.
- 예시:
```bash
nslookup example.com
```

### `traceroute`
- 기능: 네트워크 경로를 추적한다.
- 예시:
```bash
traceroute example.com
```
- 주의: 설치가 안 되어 있을 수 있다.

---

## 7. 권한과 사용자 관리

### `whoami`
- 기능: 현재 로그인한 사용자를 확인한다.
- 예시:
```bash
whoami
```

### `id`
- 기능: 현재 사용자 UID/GID와 그룹을 확인한다.
- 예시:
```bash
id
```

### `chmod`
- 기능: 파일 권한을 변경한다.
- 예시:
```bash
chmod 600 mykey.pem
chmod +x deploy.sh
```

### `chown`
- 기능: 파일 소유자/그룹을 변경한다.
- 예시:
```bash
sudo chown opc:opc /home/opc/deploy
```

### `sudo`
- 기능: 관리자 권한으로 명령을 실행한다.
- 예시:
```bash
sudo dnf update -y
sudo systemctl restart docker
```

---

## 8. 패키지 설치와 업데이트

### Oracle Linux / RHEL 계열

#### `dnf`
- 기능: 패키지 설치, 제거, 업데이트
- 예시:
```bash
sudo dnf update -y
sudo dnf install -y git
sudo dnf install -y docker
```

#### `yum`
- 기능: 구버전 계열 패키지 관리자
- 예시:
```bash
sudo yum install -y git
```

### Ubuntu / Debian 계열

#### `apt`
- 기능: 패키지 설치, 제거, 업데이트
- 예시:
```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y git
```

### `which`
- 기능: 특정 명령어가 어디에 설치되어 있는지 확인한다.
- 예시:
```bash
which docker
which git
which dnf
which apt
```

---

## 9. 압축과 파일 전송

### `tar`
- 기능: 여러 파일을 묶거나 압축 해제한다.
- 예시:
```bash
tar -czvf backup.tar.gz deploy/
tar -xzvf backup.tar.gz
```

### `scp`
- 기능: 로컬과 원격 서버 사이에 파일을 복사한다.
- 예시:
```bash
scp -i mykey.pem app.jar opc@1.2.3.4:/home/opc/
scp -r -i mykey.pem ./project opc@1.2.3.4:/home/opc/
```

### `rsync`
- 기능: 변경분 중심으로 파일을 동기화한다.
- 예시:
```bash
rsync -avz ./project opc@1.2.3.4:/home/opc/project
```

---

## 10. 로그와 장애 대응 때 자주 쓰는 조합

### 애플리케이션 로그 확인
```bash
tail -f app.log
grep "ERROR" app.log
```

### 서비스 상태와 로그 함께 보기
```bash
systemctl status docker
journalctl -u docker -f
```

### 포트와 프로세스 함께 보기
```bash
sudo ss -tulpn
ps -ef | grep java
```

### 서버 상태 빠르게 확인
```bash
df -h
free -m
top
```

---

## 11. Docker 운영에서 자주 쓰는 명령어

### `docker ps`
- 기능: 실행 중인 컨테이너 확인
- 예시:
```bash
docker ps
docker ps -a
```

### `docker logs`
- 기능: 컨테이너 로그 확인
- 예시:
```bash
docker logs my-app
docker logs -f my-app
```

### `docker exec`
- 기능: 실행 중인 컨테이너 내부에 들어가거나 명령 실행
- 예시:
```bash
docker exec -it my-app /bin/bash
docker exec -it my-app sh
```

### `docker images`
- 기능: 로컬 이미지 목록 확인
- 예시:
```bash
docker images
```

### `docker compose`
- 기능: 여러 컨테이너 서비스를 함께 관리
- 예시:
```bash
docker compose up -d
docker compose down
docker compose ps
docker compose logs -f
```

---

## 12. 인프라 작업에서 특히 자주 쓰는 시작 세트

### 서버 첫 접속 후 점검 세트
```bash
pwd
ls -al
cat /etc/os-release
df -h
free -m
timedatectl
sudo ss -tulpn
```

### 배포 전 점검 세트
```bash
which git
which docker
docker --version
docker compose version
systemctl status docker
```

### 장애 발생 시 점검 세트
```bash
df -h
free -m
top
sudo ss -tulpn
systemctl status docker
journalctl -u docker -f
docker compose ps
docker compose logs -f
```

---

## 13. 초보자가 자주 하는 실수
- `rm -rf`를 경로 확인 없이 실행한다.
- `sudo` 없이 권한이 필요한 작업을 하다가 실패 원인을 다른 곳에서 찾는다.
- 클라우드 보안 규칙만 보고 "포트가 열렸다"고 생각한다.
  - 실제로는 서버 내부 프로세스가 떠 있어야 한다.
- 서버 내부에서만 되는 요청을 외부에서도 될 거라고 생각한다.
  - `curl localhost` 성공과 외부 접속 성공은 다르다.
- 로그를 안 보고 재시작만 반복한다.
- OS 종류를 확인하지 않고 `apt`, `dnf`, `yum`을 섞어 쓴다.

---

## 14. 한 줄 요약
- 인프라 리눅스 명령어는 많이 외우는 것보다, `파일`, `권한`, `프로세스`, `서비스`, `네트워크`, `로그`, `패키지` 중 지금 어느 문제를 보고 있는지 구분하면서 쓰는 것이 핵심이다.
