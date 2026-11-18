FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/uploads
COPY --from=build /app/build/libs/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
