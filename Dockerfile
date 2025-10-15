# syntax=docker/dockerfile:1

# --- Build Stage ---
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Nur pom zuerst für besseren Cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline

# Jetzt den Rest
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
WORKDIR /app

# Robust gegen Versionsänderungen
ARG JAR_FILE=/app/target/*.jar
COPY --from=build ${JAR_FILE} /app/app.jar

EXPOSE 8080

# Hinweis: Konfiguration (DB_* , JWT_SECRET, usw.) kommt per ENV/Compose
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
