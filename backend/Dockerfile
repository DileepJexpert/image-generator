# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Resolve dependencies first for better layer caching.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# fontconfig lets the JDK's AWT font subsystem initialise cleanly in this headless image —
# the lesson PDF worksheet rasterises non-Latin (e.g. Devanagari) text lines via Java2D.
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8585
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
