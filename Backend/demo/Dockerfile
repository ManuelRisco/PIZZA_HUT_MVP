FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiar el wrapper de Gradle y archivos de configuración
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Dar permisos de ejecución al wrapper
RUN chmod +x ./gradlew

# Descargar dependencias (opcional, ayuda al caché)
# RUN ./gradlew dependencies --no-daemon

# Copiar el código fuente y construir
COPY src src
RUN ./gradlew build -x test --no-daemon

# Etapa de ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar el JAR generado de la etapa anterior
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
