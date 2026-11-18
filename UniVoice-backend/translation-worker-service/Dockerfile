FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY translation-worker-service ./translation-worker-service
RUN mvn -f translation-worker-service/pom.xml clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/translation-worker-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx380m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]