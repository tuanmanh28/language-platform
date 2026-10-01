# Image chạy backend. Jar được build trước bằng Gradle (cần Android SDK vì monorepo có module Android):
#   ./gradlew :backend:shadowJar
#   docker compose up --build
# CI build jar rồi build image này; production đẩy image lên Cloud Run / ECS.
FROM eclipse-temurin:21-jre

RUN useradd --system --create-home app
WORKDIR /app
COPY backend/build/libs/backend-all.jar /app/app.jar

USER app
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
