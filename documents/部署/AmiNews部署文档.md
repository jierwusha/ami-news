# AmiNews新闻聚合与智能摘要平台 - 部署文档

**文档版本：** v1.0  
**创建日期：** 2025年7月8日  
**适用版本：** AmiNews v0.0.1-SNAPSHOT

---

## 目录

1. [系统概述](#1-系统概述)
2. [硬件和软件要求](#2-硬件和软件要求)
3. [环境准备](#3-环境准备)
4. [Docker容器化部署](#4-docker容器化部署)
5. [手动部署](#5-手动部署)
6. [配置说明](#6-配置说明)
7. [启动和验证](#7-启动和验证)
8. [数据备份和恢复](#8-数据备份和恢复)
9. [常见问题和故障排除](#9-常见问题和故障排除)
10. [性能监控和运维](#10-性能监控和运维)

---

## 1. 系统概述

AmiNews是一个基于微服务架构的新闻聚合与智能摘要平台，采用前后端分离设计。系统主要包含以下组件：

- **前端应用**：基于Vue 3 + TypeScript的响应式Web界面
- **后端API服务**：基于Spring Boot的RESTful API服务
- **MySQL数据库**：存储用户、新闻、订阅等结构化数据
- **Redis缓存**：提供缓存和会话存储服务
- **Elasticsearch**：提供新闻向量检索和语义搜索功能
- **RSSHub服务**：提供RSS源聚合和标准化服务
- **Nginx反向代理**：负载均衡和静态资源服务

---

## 2. 硬件和软件要求

### 2.1 硬件要求

#### 最低配置
- **CPU**：2核心 2.0GHz
- **内存**：4GB RAM
- **存储**：20GB可用磁盘空间
- **网络**：稳定的互联网连接

#### 推荐配置
- **CPU**：4核心 2.5GHz或更高
- **内存**：8GB RAM或更高
- **存储**：50GB SSD
- **网络**：100Mbps带宽

#### 生产环境配置
- **CPU**：8核心 3.0GHz或更高
- **内存**：16GB RAM或更高
- **存储**：100GB+ SSD
- **网络**：1Gbps带宽

### 2.2 软件要求

#### 操作系统支持
- **Linux**：Ubuntu 20.04+, CentOS 7+, RHEL 7+
- **Windows**：Windows 10, Windows Server 2016+
- **macOS**：macOS 10.15+

#### 必需软件（Docker部署）
- **Docker**：20.10.0+
- **Docker Compose**：1.29.0+

#### 必需软件（手动部署）
- **Java**：JDK 17+
- **Node.js**：16.0+
- **MySQL**：8.0+
- **Redis**：6.0+
- **Elasticsearch**：8.8.0
- **Nginx**：1.18+

---

## 3. 环境准备

### 3.1 创建部署目录

```bash
# 创建应用部署目录
sudo mkdir -p /opt/aminews
cd /opt/aminews

# 创建数据存储目录
sudo mkdir -p /opt/aminews/data/{mysql,redis,elasticsearch}
sudo mkdir -p /opt/aminews/logs
sudo mkdir -p /opt/aminews/config
```

### 3.2 防火墙配置

```bash
# 开放必要端口
sudo ufw allow 80      # Nginx HTTP
sudo ufw allow 443     # Nginx HTTPS
sudo ufw allow 3000    # 前端应用（可选，生产环境通过Nginx代理）
sudo ufw allow 8080    # 后端API（可选，生产环境通过Nginx代理）
```

### 3.3 系统用户创建

```bash
# 创建专用系统用户
sudo useradd -r -s /bin/false aminews
sudo chown -R aminews:aminews /opt/aminews
```

---

## 4. Docker容器化部署

### 4.1 获取源代码

```bash
# 克隆项目仓库
git clone http://whucsgitlab.whu.edu.cn/2025se-aminoslite/aminews.git
cd aminews

# 或者从发布包解压
wget https://releases.example.com/aminews/v1.0.0/aminews-v1.0.0.tar.gz
tar -xzf aminews-v1.0.0.tar.gz
cd aminews
```

### 4.2 环境变量配置

创建环境配置文件：

```bash
# 复制环境配置模板
cp .env.example .env

# 编辑环境配置
nano .env
```

`.env` 文件内容示例：

```env
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=aminews
MYSQL_USER=aminews
MYSQL_PASSWORD=your_mysql_password

# Redis配置
REDIS_PASSWORD=your_redis_password

# AI服务配置
AI_API_KEY=your_ai_api_key
AI_BASE_URL=https://api.openai.com/v1

# JWT配置
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# 邮件服务配置
MAIL_HOST=smtp.qq.com
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=your_email_password
```

### 4.3 Docker Compose部署

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
```

### 4.4 服务健康检查

```bash
# 检查所有容器状态
docker-compose ps

# 检查后端API健康状态
curl http://localhost:8080/api/hello

# 检查前端应用
curl http://localhost:3000

# 检查Nginx代理
curl http://localhost
```

---

## 5. 手动部署

### 5.1 数据库部署

#### 5.1.1 MySQL安装和配置

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server

# 启动MySQL服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### 5.1.2 创建数据库和用户

```sql
-- 连接MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE aminews CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'aminews'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON aminews.* TO 'aminews'@'localhost';
FLUSH PRIVILEGES;

-- 导入数据库结构
USE aminews;
SOURCE /path/to/AmiNews.sql;
```

### 5.2 Redis部署

```bash
# 安装Redis
sudo apt install redis-server  # Ubuntu/Debian
sudo yum install redis         # CentOS/RHEL

# 配置Redis
sudo nano /etc/redis/redis.conf

# 修改以下配置
bind 127.0.0.1
port 6379
requirepass your_redis_password

# 启动Redis服务
sudo systemctl start redis
sudo systemctl enable redis
```

### 5.3 Elasticsearch部署

```bash
# 下载并安装Elasticsearch
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.8.0-linux-x86_64.tar.gz
tar -xzf elasticsearch-8.8.0-linux-x86_64.tar.gz
sudo mv elasticsearch-8.8.0 /opt/elasticsearch

# 创建elasticsearch用户
sudo useradd -r elasticsearch
sudo chown -R elasticsearch:elasticsearch /opt/elasticsearch

# 配置Elasticsearch
sudo nano /opt/elasticsearch/config/elasticsearch.yml
```

elasticsearch.yml 配置：

```yaml
cluster.name: aminews-cluster
node.name: aminews-node-1
path.data: /opt/elasticsearch/data
path.logs: /opt/elasticsearch/logs
network.host: 127.0.0.1
http.port: 9200
discovery.type: single-node
xpack.security.enabled: false
```

### 5.4 后端应用部署

```bash
# 编译后端应用
cd aminews-backend
./mvnw clean package -DskipTests

# 创建应用目录
sudo mkdir -p /opt/aminews/backend
sudo cp target/aminews-backend-0.0.1-SNAPSHOT.jar /opt/aminews/backend/

# 复制配置文件
sudo cp src/main/resources/application-prod.yaml /opt/aminews/config/

# 创建启动脚本
sudo nano /opt/aminews/backend/start.sh
```

启动脚本内容：

```bash
#!/bin/bash
cd /opt/aminews/backend
java -jar \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=/opt/aminews/config/application-prod.yaml \
  -Xmx2g -Xms1g \
  aminews-backend-0.0.1-SNAPSHOT.jar
```

### 5.5 前端应用部署

```bash
# 构建前端应用
cd aminews-frontend
npm install
npm run build

# 复制构建文件到Nginx目录
sudo cp -r dist/* /var/www/html/aminews/
```

### 5.6 Nginx配置

创建Nginx配置文件：

```bash
sudo nano /etc/nginx/sites-available/aminews
```

配置内容：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /var/www/html/aminews;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # RSSHub代理
    location /rsshub/ {
        proxy_pass http://localhost:1200;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

启用站点：

```bash
sudo ln -s /etc/nginx/sites-available/aminews /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 6. 配置说明

### 6.1 后端配置文件

主要配置文件：`application-prod.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aminews?useSSL=false&serverTimezone=Asia/Shanghai
    username: aminews
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      
  elasticsearch:
    uris: http://localhost:9200
    
  mail:
    host: smtp.qq.com
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION}

ai:
  api-key: ${AI_API_KEY}
  base-url: ${AI_BASE_URL}
```

### 6.2 前端配置

环境配置文件：`.env.production`

```env
VITE_API_BASE_URL=http://your-domain.com/api
VITE_RSSHUB_BASE_URL=http://your-domain.com/rsshub
```

---

## 7. 启动和验证

### 7.1 服务启动顺序

```bash
# 1. 启动基础服务
sudo systemctl start mysql
sudo systemctl start redis
sudo systemctl start elasticsearch

# 2. 启动应用服务
sudo systemctl start aminews-backend
sudo systemctl start nginx

# 3. 验证服务状态
sudo systemctl status mysql redis elasticsearch aminews-backend nginx
```

### 7.2 健康检查

```bash
# 检查后端API
curl http://localhost:8080/api/hello

# 检查数据库连接
curl http://localhost:8080/api/health

# 检查前端页面
curl http://localhost/

# 检查Elasticsearch
curl http://localhost:9200/_cluster/health
```

### 7.3 功能验证

1. **用户注册登录**
   - 访问 `http://your-domain.com`
   - 注册新用户账号
   - 验证邮箱验证码功能

2. **RSS订阅功能**
   - 添加RSS源
   - 验证新闻抓取功能
   - 检查定时更新任务

3. **AI功能测试**
   - 测试新闻摘要生成
   - 验证智能问答功能
   - 检查向量检索性能

---

## 8. 数据备份和恢复

### 8.1 数据库备份

```bash
# 创建备份脚本
sudo nano /opt/aminews/scripts/backup.sh
```

备份脚本内容：

```bash
#!/bin/bash
BACKUP_DIR="/opt/aminews/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# MySQL备份
mysqldump -u aminews -p aminews > $BACKUP_DIR/mysql_$DATE.sql

# Redis备份
cp /var/lib/redis/dump.rdb $BACKUP_DIR/redis_$DATE.rdb

# Elasticsearch备份
curl -X PUT "localhost:9200/_snapshot/backup_repo/snapshot_$DATE"

# 清理7天前的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.rdb" -mtime +7 -delete

echo "Backup completed: $DATE"
```

### 8.2 定时备份

```bash
# 添加到crontab
crontab -e

# 每天凌晨2点备份
0 2 * * * /opt/aminews/scripts/backup.sh
```

### 8.3 数据恢复

```bash
# MySQL恢复
mysql -u aminews -p aminews < backup_file.sql

# Redis恢复
sudo systemctl stop redis
cp backup_file.rdb /var/lib/redis/dump.rdb
sudo systemctl start redis

# Elasticsearch恢复
curl -X POST "localhost:9200/_snapshot/backup_repo/snapshot_name/_restore"
```

---

## 9. 常见问题和故障排除

### 9.1 常见问题

#### 9.1.1 服务无法启动

**问题**：后端服务启动失败
```bash
# 检查日志
sudo journalctl -u aminews-backend -f

# 检查端口占用
sudo netstat -tulpn | grep :8080

# 检查配置文件
java -jar aminews-backend.jar --spring.config.location=application-prod.yaml --dry-run
```

**解决方案**：
- 检查配置文件语法
- 验证数据库连接
- 确认端口未被占用

#### 9.1.2 数据库连接失败

**问题**：应用无法连接数据库
```bash
# 检查MySQL状态
sudo systemctl status mysql

# 测试连接
mysql -u aminews -p -h localhost
```

**解决方案**：
- 检查MySQL服务状态
- 验证用户权限
- 检查防火墙设置

#### 9.1.3 内存不足

**问题**：应用运行时内存不足
```bash
# 检查内存使用
free -h
top -p $(pgrep java)
```

**解决方案**：
- 调整JVM内存参数
- 优化应用配置
- 升级硬件配置

### 9.2 日志分析

#### 9.2.1 日志位置

```bash
# 应用日志
/opt/aminews/logs/application.log

# Nginx日志
/var/log/nginx/access.log
/var/log/nginx/error.log

# MySQL日志
/var/log/mysql/error.log

# 系统日志
sudo journalctl -u aminews-backend
```

#### 9.2.2 常见错误

**错误1**：`Connection refused`
- 检查目标服务是否启动
- 验证网络连接
- 检查防火墙设置

**错误2**：`OutOfMemoryError`
- 增加JVM堆内存
- 优化应用代码
- 检查内存泄漏

**错误3**：`Authentication failed`
- 检查用户名密码
- 验证权限配置
- 检查认证服务状态

---

## 10. 性能监控和运维

### 10.1 性能监控

#### 10.1.1 系统监控

```bash
# 安装监控工具
sudo apt install htop iotop nethogs

# 实时监控
htop                    # CPU和内存
iotop                   # 磁盘I/O
nethogs                 # 网络使用
```

#### 10.1.2 应用监控

```bash
# JVM监控
jstat -gc $(pgrep java) 1s

# 线程监控
jstack $(pgrep java)

# 内存分析
jmap -heap $(pgrep java)
```

### 10.2 性能优化

#### 10.2.1 JVM调优

```bash
# 优化启动参数
java -jar \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+PrintGCDetails \
  aminews-backend.jar
```

#### 10.2.2 数据库优化

```sql
-- MySQL优化配置
SET GLOBAL innodb_buffer_pool_size = 1073741824;  -- 1GB
SET GLOBAL max_connections = 200;
SET GLOBAL query_cache_size = 67108864;           -- 64MB
```

#### 10.2.3 缓存优化

```bash
# Redis优化配置
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### 10.3 日常运维

#### 10.3.1 定期维护

```bash
# 创建维护脚本
sudo nano /opt/aminews/scripts/maintenance.sh
```

维护脚本内容：

```bash
#!/bin/bash

# 清理日志文件
find /opt/aminews/logs -name "*.log" -mtime +30 -delete

# 优化数据库
mysql -u aminews -p -e "OPTIMIZE TABLE items, channels, users;"

# 清理Redis过期键
redis-cli FLUSHEXPIRED

# 重启服务（如需要）
# sudo systemctl restart aminews-backend

echo "Maintenance completed: $(date)"
```

#### 10.3.2 安全更新

```bash
# 系统更新
sudo apt update && sudo apt upgrade

# 检查漏洞
sudo apt install lynis
sudo lynis audit system
```

---

## 附录

### A. 端口使用说明

| 服务 | 端口 | 描述 |
|------|------|------|
| Nginx | 80, 443 | Web服务器 |
| 后端API | 8080 | Spring Boot应用 |
| 前端应用 | 3000 | Vue.js开发服务器 |
| MySQL | 3306 | 数据库服务 |
| Redis | 6379 | 缓存服务 |
| Elasticsearch | 9200 | 搜索引擎 |
| RSSHub | 1200 | RSS聚合服务 |

### B. 目录结构

```
/opt/aminews/
├── backend/
│   ├── aminews-backend.jar
│   └── start.sh
├── config/
│   ├── application-prod.yaml
│   └── nginx.conf
├── data/
│   ├── mysql/
│   ├── redis/
│   └── elasticsearch/
├── logs/
│   ├── application.log
│   └── access.log
├── scripts/
│   ├── backup.sh
│   └── maintenance.sh
└── backups/
    ├── mysql_*.sql
    └── redis_*.rdb
```

### C. 服务依赖关系

```
Frontend (Vue.js)
    ↓
Nginx (Reverse Proxy)
    ↓
Backend (Spring Boot)
    ↓
├── MySQL (Database)
├── Redis (Cache)
├── Elasticsearch (Search)
└── RSSHub (RSS Aggregation)
```

---

**文档维护**：请定期更新本文档以反映系统的最新变化和最佳实践。

**技术支持**：如遇到部署问题，请联系开发团队或查看项目Wiki。
