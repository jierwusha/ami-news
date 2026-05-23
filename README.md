# AmiNews

AmiNews 是一个新闻聚合平台，包含：

- `aminews-frontend`：Vue 3 + Vite 前端
- `aminews-backend`：Spring Boot 3 后端
- `docker-compose.yml`：本地一键启动依赖与服务

## 环境要求

- Node.js 20+
- npm 10+
- Java 17
- Docker / Docker Compose（用于完整联调）

## 快速开始（前后端分开启动）

### 1) 前端

```bash
cd /home/runner/work/ami-news/ami-news/aminews-frontend
npm install
npm run dev
```

常用命令：

```bash
npm run build
npm run lint
npm run test:unit -- --run
```

> 当前仓库中前端 `lint` 存在历史问题（与本次构建修复无关），`build` 可正常通过。

### 2) 后端

```bash
cd /home/runner/work/ami-news/ami-news/aminews-backend
./mvnw -DskipTests package
./mvnw spring-boot:run
```

> 后端测试依赖 MySQL / Redis / Elasticsearch 等外部服务，未启动依赖时测试会失败。

## 使用 Docker Compose 启动完整环境

```bash
cd /home/runner/work/ami-news/ami-news
docker compose up --build
```

默认端口（详见 `docker-compose.yml`）：

- Nginx: `80`
- Frontend: `3000`
- Backend: `8080`
- MySQL: `3306`
- Redis: `6379`
- Elasticsearch: `9200`

## 本次构建修复

- 后端 Maven 父版本从 `3.4.8-SNAPSHOT` 调整为稳定版 `3.4.8`，避免依赖快照仓库导致的构建失败。
- `aminews-backend/mvnw` 设置为可执行，支持直接执行 `./mvnw`。
