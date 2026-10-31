# ---- Giai đoạn build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# ---- Giai đoạn chạy ----
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 appuser
WORKDIR /app
COPY --from=build /app/target/commentary-backend.jar app.jar
USER appuser
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    SERVER_PORT=3000
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:${SERVER_PORT}/api/user/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
