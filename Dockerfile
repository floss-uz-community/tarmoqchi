FROM gradle:7.6-jdk17 AS build

WORKDIR /app

# Only copy what's needed for the build
COPY server/build.gradle server/settings.gradle ./
COPY server/src ./src/

# Run build
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the specific JAR file to avoid wildcard issues
COPY --from=build /app/build/libs/server-0.0.1-SNAPSHOT.jar app.jar

# Expose the service port
EXPOSE 7140

ENTRYPOINT ["java", "-jar", "app.jar"]
