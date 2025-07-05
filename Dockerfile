# Base image with JDK 17
FROM eclipse-temurin:17-jdk-jammy AS builder

# Set working dir
WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build JAR file (fat jar)
RUN ./gradlew clean bootJar

# ---- RUNTIME STAGE ----
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy JAR file from builder image
COPY --from=builder /app/build/libs/*.jar app.jar

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
