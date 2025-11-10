# Multi-stage build for Java Spring Boot application

# Stage 1: Build stage
FROM oraclelinux:10 AS builder

# Install OpenJDK 21 and Maven
RUN dnf update -y && \
    dnf install -y java-21-openjdk-devel maven git && \
    dnf clean all

# Set working directory
WORKDIR /app

# Copy Maven settings if provided (for CI/CD with authentication)
# This allows optional settings.xml - will be ignored if not present
COPY maven-settings.xml* /tmp/
RUN if [ -f /tmp/maven-settings.xml ]; then \
        mkdir -p /root/.m2 && \
        cp /tmp/maven-settings.xml /root/.m2/settings.xml; \
    fi

# Copy pom.xml and source code  
COPY pom.xml .
COPY src ./src

# Build the application
ARG APP_VERSION
ARG BUILD_PHASE=package
ARG SKIP_TESTS=true
RUN if [ -n "$APP_VERSION" ]; then \
        mvn versions:set -DnewVersion=${APP_VERSION} -DgenerateBackupPoms=false; \
    fi && \
    if [ "$SKIP_TESTS" = "true" ]; then \
        mvn clean ${BUILD_PHASE} -DskipTests; \
    else \
        mvn clean test ${BUILD_PHASE}; \
    fi

FROM oraclelinux:10 AS runtime

# Add Maintainer Info
LABEL maintainer="adam.stegienko1@gmail.com"

# Install OpenJDK 21 JRE (headless) and wget for health check
RUN dnf update -y && \
    dnf install -y java-21-openjdk-headless wget && \
    dnf clean all

# Create non-root user for security
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -u 1001 appuser

# Set working directory
WORKDIR /app

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8000 available to the world outside this container
EXPOSE 8000

# Set default JAVA_OPTS (can be overridden at runtime)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Copy the jar file from builder stage
ARG APP_VERSION
COPY --from=builder /app/target/campaign_controller_api_rest-*.jar campaign_controller_api_rest.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# # Health check (using your root endpoint)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#     CMD wget --no-verbose --tries=1 --spider http://localhost:8000/ || exit 1

# Run the jar file with configurable JVM settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar campaign_controller_api_rest.jar"]
