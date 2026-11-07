# Build do artefato da app Spring Boot
FROM --platform=linux/amd64 gradle:9.4.1-jdk21-corretto AS build

COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src

RUN gradle :app:clean :app:bootJar --no-daemon -x test

# Configuracao da imagem de runtime
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build /home/gradle/src/app/build/libs/*.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
