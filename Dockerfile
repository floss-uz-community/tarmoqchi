FROM openjdk:17-jdk-slim

WORKDIR /app

COPY /server/build/libs/server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8079

ENTRYPOINT ["java", "-jar", "app.jar"]