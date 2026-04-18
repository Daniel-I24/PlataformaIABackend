# ---- Etapa 1: Build con Maven ----
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copio primero solo el pom.xml para aprovechar el cache de capas de Docker
# Si el pom.xml no cambia, Docker reutiliza la capa de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copio el código fuente y compilo
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Etapa 2: Imagen final ligera ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copio solo el JAR generado — la imagen final no incluye Maven ni el código fuente
COPY --from=build /app/target/ai-proxy-backend-1.0.0.jar app.jar

# Puerto que expone la aplicación (Railway usa la variable PORT)
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
