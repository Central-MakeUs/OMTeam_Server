FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

ARG PROFILES
ARG ENV

ENTRYPOINT ["java", "-Dserver.profiles.active=${PROFILES}", "-Dserver.env=${ENV}" , "-jar", "app.jar"]
