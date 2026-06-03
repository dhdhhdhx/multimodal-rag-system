# ============================================================
# Stage 1: Maven build
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 配置 Maven：阿里云镜像仅用于 central，Spring 官方仓库保留
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\n\
<settings>\n\
  <mirrors>\n\
    <mirror>\n\
      <id>aliyunmaven</id>\n\
      <name>Aliyun Maven</name>\n\
      <url>https://maven.aliyun.com/repository/public</url>\n\
      <mirrorOf>central</mirrorOf>\n\
    </mirror>\n\
  </mirrors>\n\
  <profiles>\n\
    <profile>\n\
      <id>spring-milestones</id>\n\
      <repositories>\n\
        <repository>\n\
          <id>spring-milestones</id>\n\
          <name>Spring Milestones</name>\n\
          <url>https://repo.spring.io/milestone</url>\n\
        </repository>\n\
      </repositories>\n\
    </profile>\n\
  </profiles>\n\
  <activeProfiles>\n\
    <activeProfile>spring-milestones</activeProfile>\n\
  </activeProfiles>\n\
</settings>' > /root/.m2/settings.xml

# Layer 1: dependencies (cached unless pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Layer 2: source code
COPY src ./src
RUN mvn clean package -DskipTests -q \
    && mv target/*.jar target/app.jar

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="multimodal-rag"
WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && mkdir -p /app/uploads /app/logs \
    && chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /app/target/app.jar app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
