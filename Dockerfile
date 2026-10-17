# ===============================
# Stage 1: Build the application
# ===============================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and config files to cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Ensure gradlew has execution rights
RUN chmod +x ./gradlew

# Download dependencies (cached as long as build files do not change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build the executable war/jar
COPY src src
RUN ./gradlew bootWar -x test --no-daemon

# ===============================
# Stage 2: Runtime image
# ===============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy built artifact from builder stage
COPY --from=builder /app/build/libs/SBMTServer-*.war app.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
