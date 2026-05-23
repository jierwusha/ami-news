FROM node:18-alpine as builder

WORKDIR /app

# 使用官方源，避免镜像同步问题
RUN npm config set registry https://registry.npmjs.org/

# 只复制依赖文件
COPY package*.json ./

# 清理缓存和锁文件
RUN rm -rf node_modules package-lock.json
RUN npm cache clean --force

# 安装依赖
RUN npm install

# 复制全部源代码
COPY . .

# 修复所有 bin 权限
RUN chmod -R +x node_modules/.bin

# 构建
RUN npm run build

FROM nginx:alpine

RUN apk add --no-cache wget

COPY --from=builder /app/dist /usr/share/nginx/html

RUN echo 'server { \
    listen 3000; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]