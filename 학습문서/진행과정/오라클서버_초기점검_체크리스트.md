# Oracle 인스턴스 첫 점검 체크리스트

## 이 문서의 목적
- 오라클 클라우드 인스턴스에 처음 접속한 뒤, 지금 서버 상태를 확인하고 기록하는 순서를 바로 실행할 수 있게 정리한다.
- 목표는 "뭘 해야 하지?"에서 멈추지 않고, 가장 작은 점검부터 시작해서 Docker 설치 준비 단계까지 자연스럽게 이어지게 하는 것이다.

## 왜 이 순서로 시작하는가
- 배포는 코드만 올린다고 끝나지 않고, 운영체제, 디스크, 메모리, 시간 설정, 포트 상태 같은 런타임 환경 위에서 돌아간다.
- 즉 서버 상태를 먼저 알아야 이후에 Docker가 안 뜨는 문제, 포트 충돌, 메모리 부족, 시간대 문제를 분리해서 볼 수 있다.
- 이 단계는 "배포" 이전에 "배포할 수 있는 환경인지 확인"하는 단계다.

## 시작 순서

### 1단계. 현재 위치와 OS 정보 확인
```bash
pwd
ls -al
uname -a
cat /etc/os-release
```

### 2단계. 디스크, 메모리, 시간 확인
```bash
df -h
free -m
timedatectl
```

### 3단계. 프로세스와 열려 있는 포트 확인
```bash
ps -ef | head -20
ss -tulpn
sudo ss -tulpn
```

### 4단계. 시스템 서비스 상태 확인
```bash
systemctl list-units --type=service --state=running
systemctl status sshd
```

### 5단계. 패키지 관리자 확인
```bash
which dnf
which yum
which apt
```

## 결과를 어떻게 읽어야 하는가

### `cat /etc/os-release`
- 현재 서버가 Oracle Linux인지 Ubuntu인지 확인한다.
- 이 값에 따라 이후 패키지 설치 명령이 달라진다.

### `df -h`
- 루트 디스크(`/`) 여유 공간이 충분한지 본다.
- Docker 이미지와 컨테이너, 로그, 볼륨이 쌓이기 때문에 여유 공간이 너무 작으면 나중에 배포가 자주 막힌다.

### `free -m`
- 메모리가 얼마나 있는지 확인한다.
- MySQL, Redis, Kafka까지 함께 띄우면 메모리 사용량이 커질 수 있으니, 지금 스펙을 먼저 알아야 이후 구성 결정을 할 수 있다.

### `timedatectl`
- 서버 시간대가 `Asia/Seoul`인지 확인한다.
- 시간대가 틀리면 로그 시간, JWT 만료 시간, 스케줄러 동작 해석이 꼬일 수 있다.

### `ss -tulpn`
- 현재 어떤 포트가 실제로 열려 있는지 확인한다.
- 오라클 보안 규칙에서 포트를 열어도, 서버 내부에서 프로세스가 안 떠 있으면 서비스는 접근되지 않는다.
- 반대로 서버 내부에서 프로세스가 떠 있어도 보안 규칙이 막고 있으면 외부 접근이 안 된다.

## 점검 후 바로 문서로 남길 항목

아래 항목은 `학습문서/진행과정/` 아래 새 문서에 바로 적는다.

- 서버 OS 종류
- 디스크 용량
- 메모리 크기
- 시간대
- 현재 실행 중인 주요 서비스
- 현재 서버 내부에서 열려 있는 포트
- 현재 오라클 보안 규칙에서 외부 공개한 포트
- 그 포트를 왜 열어뒀는지
- 이후 닫아야 할 포트 후보

## 그 다음 단계: 설치 준비

OS 확인이 끝났으면 그다음은 패키지 업데이트와 필수 도구 설치로 넘어간다.

### Oracle Linux / RHEL 계열일 가능성이 높을 때
```bash
sudo dnf update -y
sudo dnf install -y git
```

### Ubuntu 계열일 때
```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y git
```

## 바로 이어서 만들 작업 디렉토리 예시
```bash
mkdir -p ~/deploy
mkdir -p ~/deploy/ubot
cd ~/deploy/ubot
pwd
```

## 지금 시점의 목표
- 아직 Docker 설치까지 한 번에 끝내는 것이 핵심이 아니다.
- 먼저 서버 상태를 이해하고 기록하는 것이 첫 목표다.
- 이 기록이 있어야 다음에 Docker 설치, 수동 배포, 포트 정리, Nginx 연결까지 흔들리지 않는다.

## 한 줄 시작 가이드
- 지금 당장은 SSH로 접속한 뒤 `cat /etc/os-release -> df -h -> free -m -> timedatectl -> sudo ss -tulpn` 순서로 실행하면서 결과를 기록하면 된다.
