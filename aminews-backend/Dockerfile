FROM openjdk:17-jdk-slim

WORKDIR /app

# 安装 curl 用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 复制 jar 文件
COPY target/*.jar app.jar

COPY src/main/resources/default_subscribe_list.json /default_subscribe_list.json

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]