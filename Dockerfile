# ===== build stage =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 의존성 캐시 최적화: pom 먼저 복사
COPY pom.xml .
COPY pes-common/pom.xml          pes-common/
COPY pes-messaging/pom.xml       pes-messaging/
COPY pes-idempotency/pom.xml     pes-idempotency/
COPY pes-lot-service/pom.xml     pes-lot-service/
COPY pes-wf-service/pom.xml      pes-wf-service/
COPY pes-durable-service/pom.xml pes-durable-service/
COPY pes-hub/pom.xml             pes-hub/
COPY pes-hub-jdbc/pom.xml        pes-hub-jdbc/
COPY pes-app/pom.xml             pes-app/
RUN mvn -q -B -pl pes-app -am dependency:go-offline || true

# 소스 복사 후 빌드 (rv-tibco 모듈은 기본 프로파일에서 제외 → 상용 jar 불필요)
COPY . .
RUN mvn -q -B -pl pes-app -am -DskipTests package

# ===== runtime stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# 헬스체크용 curl + 비루트 사용자
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r pes && useradd -r -g pes pes

COPY --from=build /build/pes-app/target/pes-app-*.jar /app/app.jar
RUN chown -R pes:pes /app
USER pes

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
