# Backend runtime image. Build the jar first with Gradle (needs the Android SDK, as the monorepo has Android modules):
#   ./gradlew :backend:shadowJar
#   docker compose up --build
# CI builds the jar, then this image; production pushes the image to Cloud Run / ECS.
FROM eclipse-temurin:21-jre

RUN useradd --system --create-home app
WORKDIR /app
COPY backend/build/libs/backend-all.jar /app/app.jar

USER app
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
