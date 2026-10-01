# Backend runtime image. Build the jar first with Gradle (needs the Android SDK, as the monorepo has Android modules):
#   ./gradlew :backend:shadowJar
#   docker compose up --build
# Deploys use backend/Dockerfile.multistage instead, which builds the jar inside Docker (docs/deploy.md).
FROM eclipse-temurin:21-jre

RUN useradd --system --create-home app
WORKDIR /app
COPY backend/build/libs/backend-all.jar /app/app.jar

USER app
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
