# 빌드 스테이지: Gradle 8.13 + JDK 17 alpine 이미지 사용
# gradlew 불필요 - 이미지에 Gradle이 내장되어 있음
FROM gradle:8.13-jdk17-alpine AS builder

WORKDIR /app

# 의존성 파일만 먼저 복사해서 캐시 레이어 분리
# build.gradle이 바뀌지 않으면 아래 dependencies 단계가 캐시 재사용됨
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 소스코드 복사 후 빌드
COPY src src
RUN gradle bootJar --no-daemon

# 실행 스테이지: JRE만 있으면 충분
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# 빌드 스테이지에서 생성된 jar만 가져옴
COPY --from=builder /app/build/libs/*.jar app.jar

# 외부 접속 포트 지정 (스프링부트 기본 8080)
EXPOSE 8080

# 애플리케이션 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]