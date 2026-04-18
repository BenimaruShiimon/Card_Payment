# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Копируем JAR из builder stage
COPY --from=builder /build/target/*.jar app.jar

# Создаём папку для логов
RUN mkdir -p /tmp/deploy/logs

EXPOSE 5500

ENTRYPOINT ["java", "-jar", "app.jar"]