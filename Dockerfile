FROM gradle:7.6-jdk17 AS build

WORKDIR /app

# Only copy what's needed for the build
COPY server/build.gradle server/settings.gradle ./
COPY server/src ./src/

# Run build
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy specific JAR file with a wildcard pattern to be more explicit
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the service port
EXPOSE 7140

ENTRYPOINT ["java", "-jar", "app.jar"]
