FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY chat-service ./chat-service
RUN mvn -f chat-service/pom.xml clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/chat-service/target/*.jar app.jar
COPY chat-service/src/main/resources/ca.pem /app/ca.pem
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx320m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]