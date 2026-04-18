FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY /target/*.jar /app/app.jar

RUN mkdir -p /tmp/deploy/logs

EXPOSE 5500

ENTRYPOINT ["java", "-jar", "/app/app.jar"]