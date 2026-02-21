FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/musicbackend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=qa"]
