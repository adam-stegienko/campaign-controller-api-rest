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
COPY maven-settings.xml* /tmp/
RUN mkdir -p /root/.m2 && \
    if [ -f /tmp/maven-settings.xml ]; then \
        cp /tmp/maven-settings.xml /root/.m2/settings.xml; \
        echo "Using provided Maven settings"; \
    else \
        echo "No Maven settings provided, creating fallback settings with multiple mirrors"; \
        cat > /root/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>central-fallback</id>
      <mirrorOf>central</mirrorOf>
      <name>Maven Central Fallback</name>
      <url>https://repo1.maven.org/maven2</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>fallback-repos</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>spring-releases</id>
          <url>https://repo.spring.io/release</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>fallback-repos</activeProfile>
  </activeProfiles>
</settings>
EOF
    fi

# Copy Maven repository cache if provided (for offline builds)
COPY .m2-repository* /tmp/
RUN if [ -d /tmp/.m2-repository ]; then \
        mkdir -p /root/.m2 && \
        cp -r /tmp/.m2-repository /root/.m2/repository; \
        echo "Copied Maven repository cache for offline builds"; \
    fi

# Copy pom.xml first for dependency resolution
COPY pom.xml .

# Download dependencies first (with fallback repositories)
RUN mvn dependency:go-offline -B \
    -Dmaven.repo.local=/root/.m2/repository \
    -Dmaven.artifact.threads=5 \
    || echo "Primary dependency download failed, will retry during build"

# Copy source code
COPY src ./src

# Build the application
ARG APP_VERSION
ARG BUILD_PHASE=package
ARG SKIP_TESTS=true

# Build the application with resilient dependency handling
# Try offline first, then fallback to online with retries
RUN set -e; \
    echo "Attempting Maven build with offline mode first..."; \
    if [ "$SKIP_TESTS" = "true" ]; then \
        mvn clean ${BUILD_PHASE} -DskipTests -o -B 2>/dev/null || { \
            echo "Offline build failed, retrying with online mode..."; \
            mvn clean ${BUILD_PHASE} -DskipTests -B -U \
                -Dmaven.artifact.threads=5 \
                -Dhttp.keepAlive=false \
                -Dmaven.wagon.http.pool=false \
                -Dmaven.wagon.http.retryHandler.count=3 \
                -Dmaven.wagon.httpconnectionManager.ttlSeconds=120; \
        }; \
    else \
        mvn clean test ${BUILD_PHASE} -o -B 2>/dev/null || { \
            echo "Offline build with tests failed, retrying with online mode..."; \
            mvn clean test ${BUILD_PHASE} -B -U \
                -Dmaven.artifact.threads=5 \
                -Dhttp.keepAlive=false \
                -Dmaven.wagon.http.pool=false \
                -Dmaven.wagon.http.retryHandler.count=3 \
                -Dmaven.wagon.httpconnectionManager.ttlSeconds=120; \
        }; \
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
