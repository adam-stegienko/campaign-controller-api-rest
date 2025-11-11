# Multi-stage build for Java Spring Boot application

# Stage 1: Build stage
FROM oraclelinux:10 AS builder

# Install OpenJDK 21 and Maven
RUN dnf update -y && \
    dnf install -y java-21-openjdk-devel maven && \
    dnf clean all

# Set working directory
WORKDIR /app

# Create simple Maven settings using only Maven Central
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings>' >> /root/.m2/settings.xml && \
    echo '  <mirrors>' >> /root/.m2/settings.xml && \
    echo '    <mirror>' >> /root/.m2/settings.xml && \
    echo '      <id>central</id>' >> /root/.m2/settings.xml && \
    echo '      <mirrorOf>*</mirrorOf>' >> /root/.m2/settings.xml && \
    echo '      <url>https://repo1.maven.org/maven2</url>' >> /root/.m2/settings.xml && \
    echo '    </mirror>' >> /root/.m2/settings.xml && \
    echo '  </mirrors>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# Copy application files
COPY pom.xml .
COPY src ./src

# Build the application
ARG SKIP_TESTS=true
RUN if [ "$SKIP_TESTS" = "true" ]; then \
        mvn clean package -DskipTests; \
    else \
        mvn clean test package; \
    fi

# Stage 2: Runtime stage
FROM oraclelinux:10 AS runtime

# Install Java runtime
RUN dnf update -y && \
    dnf install -y java-21-openjdk-headless && \
    dnf clean all

# Create app user
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -u 1001 appuser

# Set working directory
WORKDIR /app

# Create writable temp directory for Tomcat
RUN chown -R appuser:appgroup /app

# Expose port
EXPOSE 8000

# Set Java options with custom temp directory
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Copy jar from build stage
COPY --from=builder /app/target/campaign_controller_api_rest-*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to app user
USER appuser

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]