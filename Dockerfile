FROM gradle:7.6-jdk17 AS build

WORKDIR /app

COPY . .
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 7140

ENTRYPOINT ["java", "-jar", "app.jar"]
