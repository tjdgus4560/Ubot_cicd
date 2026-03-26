# 빌드 스테이지
FROM gradle:8.13-jdk17 AS builder

# 컨테이너 내 작업 디렉토리
WORKDIR /app

# Gradle 캐시 활용을 위해 의존성 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon || return 0

# 이후 전체 복사 및 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# 런타임 스테이지 (경량 OpenJDK)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 위에서 빌드된 JAR 파일 복사
ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=builder ${JAR_FILE} app.jar

# 외부 접속 포트 지정 (스프링부트 기본 8080)
EXPOSE 8080

# 애플리케이션 실행 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
