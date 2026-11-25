
# 1. Java 17 환경 준비
FROM eclipse-temurin:17-jdk AS build

# 2. 작업 디렉터리 설정
WORKDIR /app

# 3. Gradle 관련 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 4. Gradle 실행 권한 부여
RUN chmod +x gradlew

# 5. 소스 코드 복사
COPY src src

# 6. Spring Boot 빌드
RUN ./gradlew clean bootJar -x test --no-daemon

# ------------------------------

# 7. 실제 실행 환경 구성
FROM eclipse-temurin:17-jre

WORKDIR /app

# 8. 빌드된 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 9. Spring Boot 포트
EXPOSE 8080

# 10. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
