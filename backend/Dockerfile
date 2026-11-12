# syntax=docker/dockerfile:1

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -e -DskipTests clean package

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /build/target/scenic-ai-backend-*.jar app.jar
COPY assets/knowledge_raw /app/assets/knowledge_raw

RUN groupadd --system lvling \
    && useradd --system --gid lvling lvling \
    && mkdir -p /data/scenic-ai/uploads /data/scenic-ai/logs \
    && chown -R lvling:lvling /app /data/scenic-ai

USER lvling

ENV KNOWLEDGE_RAW_PATH=/app/assets/knowledge_raw \
    FILE_UPLOAD_PATH=/data/scenic-ai/uploads \
    LOGGING_FILE_PATH=/data/scenic-ai/logs

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
