# Multimodal RAG System 部署指南

## 目录

- [前提条件](#前提条件)
- [一、本地部署（Docker Compose）](#一本地部署docker-compose)
- [二、阿里云 ECS 部署](#二阿里云-ecs-部署)
- [三、常用运维命令](#三常用运维命令)
- [四、常见问题](#四常见问题)

---

## 前提条件

| 依赖 | 最低版本 | 检查命令 |
|------|---------|---------|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |
| Git | 2.x | `git --version` |

> 如果你还没有安装 Docker，请参考 [Docker 官方安装文档](https://docs.docker.com/get-docker/)。

---

## 一、本地部署（Docker Compose）

### 1. 克隆代码

```bash
git clone <你的仓库地址>
cd multimodal-rag-system
```

### 2. 配置环境变量

```bash
cp deploy/.env.example .env
```

用编辑器打开 `.env` 文件，**必须修改**以下项：

```bash
# ====== 必填 ======

# MySQL root 密码（自己定一个强密码）
MYSQL_ROOT_PASSWORD=MyStr0ngP@ssw0rd!

# PgVector 密码
PGVECTOR_PASSWORD=MyPgP@ssw0rd!

# OpenAI 兼容 API Key（使用阿里云 DashScope）
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx

# JWT 密钥（随机字符串，至少 32 位）
JWT_SECRET=my-random-secret-key-at-least-256-bits-long!!

# ====== 按需填写 ======

# Redis 密码（留空则无密码）
REDIS_PASSWORD=

# 前端端口（默认 80）
FRONTEND_PORT=80

# 阿里云 OSS（不用云存储可留空）
ALIYUN_OSS_ENDPOINT=
ALIYUN_OSS_ACCESS_KEY_ID=
ALIYUN_OSS_ACCESS_KEY_SECRET=
ALIYUN_OSS_BUCKET_NAME=
```

### 3. 启动所有服务

```bash
docker compose up -d --build
```

首次启动会做以下事情：
- 下载 MySQL 8.0、PostgreSQL 16 + PgVector、Redis 7 镜像
- 编译 Java 后端（Maven 打包）
- 编译 Vue 前端（npm build）
- 下载 Python AI 模型（首次较慢，约 1-2GB，后续使用缓存）

> **Python 模型下载**：首次启动时 Python 服务需要下载 3 个模型（文本嵌入、图片 CLIP、音频 Whisper），国内环境通过 `hf-mirror.com` 镜像加速。进度可通过 `docker compose logs -f python-service` 查看。

### 4. 验证部署

```bash
# 查看所有容器状态（应全部为 healthy）
docker compose ps

# 查看后端健康检查
curl http://localhost:8080/api/health
# 预期输出: {"status":"UP"}
```

打开浏览器访问：

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost |
| 后端 API | http://localhost:8080 |
| Python AI 服务 | http://localhost:8000 |
| API 文档 (Swagger) | http://localhost:8080/swagger-ui.html |

### 5. 注册第一个用户

打开 http://localhost ，使用注册功能创建账号。第一个注册的用户默认角色为 USER。

如需创建管理员账号，进入 MySQL 容器手动修改：

```bash
docker exec -it multimodal-mysql mysql -uroot -p

# 输入你在 .env 中设置的 MYSQL_ROOT_PASSWORD，然后执行：
USE knowledge_management;
UPDATE user SET roles = 'ADMIN' WHERE username = '你的用户名';
```

---

## 二、阿里云 ECS 部署

### 方案 A：ECS 上直接构建（推荐新手）

适合第一次部署、服务器配置不高的情况。

#### 1. 连接 ECS

```bash
ssh root@<你的ECS公网IP>
```

#### 2. 安装 Docker

```bash
# CentOS / Alibaba Cloud Linux
curl -fsSL https://get.docker.com | sh
systemctl enable docker && systemctl start docker

# 验证
docker --version
docker compose version
```

#### 3. 传输代码到 ECS

```bash
# 在你本地电脑执行
scp -r ./multimodal-rag-system root@<ECS公网IP>:/root/
```

或者直接在 ECS 上 clone：

```bash
cd /root
git clone <你的仓库地址>
cd multimodal-rag-system
```

#### 4. 配置并启动

```bash
cp deploy/.env.example .env
vi .env
# 填入真实密码和 API Key（参考本地部署的第 2 步）

# 一键部署
bash deploy/ecs-deploy.sh
```

部署完成后，通过 `http://<ECS公网IP>` 访问系统。

> **安全组配置**：确保阿里云安全组开放了 80 端口（前端）和 22 端口（SSH）。不需要开放 8080、3306、5432 等端口，这些只在 Docker 内部网络通信。

#### 5. 配置域名（可选）

如果你有域名，在域名服务商处添加 A 记录指向 ECS 公网 IP。本项目的 `deploy/nginx.conf` 已经配置好了 `/api/` 反向代理，只需将域名指向 80 端口。

---

### 方案 B：本地构建 + 推送 ACR（适合 CI/CD）

适合团队协作、多次部署的场景。

#### 1. 登录阿里云容器镜像服务 (ACR)

```bash
# 在阿里云控制台 → 容器镜像服务 → 实例 → 创建个人实例（免费）
# 创建命名空间（如 multimodal）
# 创建 3 个镜像仓库：multimodal-java-backend、multimodal-python-service、multimodal-vue-frontend

docker login your-registry.cn-hangzhou.cr.aliyuncs.com
# 输入 ACR 用户名和密码
```

#### 2. 配置 .env 中的 ACR 信息

```bash
ACR_REGISTRY=your-registry.cn-hangzhou.cr.aliyuncs.com
ACR_NAMESPACE=multimodal
ACR_IMAGE_TAG=latest
```

#### 3. 构建并推送镜像

```bash
bash deploy/push-to-acr.sh
```

脚本会自动构建 3 个服务的 Docker 镜像并推送到 ACR。

#### 4. 在 ECS 上拉取并启动

```bash
# 在 ECS 上
cp deploy/.env.example .env
vi .env
# 填入 ACR_REGISTRY 和 ACR_NAMESPACE

bash deploy/ecs-deploy.sh
```

脚本会检测到 ACR 配置，自动从 ACR 拉取镜像而不是本地构建。

#### 5. 后续更新代码

```bash
# 本地：推送新镜像
bash deploy/push-to-acr.sh

# ECS：拉取并重启
docker compose -f docker-compose.yml -f deploy/docker-compose.ecs.yml pull
docker compose -f docker-compose.yml -f deploy/docker-compose.ecs.yml up -d
```

---

## 三、常用运维命令

### 查看状态

```bash
# 所有容器运行状态
docker compose ps

# 查看后端日志
docker compose logs -f java-backend

# 查看 Python AI 服务日志
docker compose logs -f python-service

# 查看前端日志
docker compose logs -f vue-frontend

# 查看所有服务日志
docker compose logs -f
```

### 重启服务

```bash
# 重启单个服务
docker compose restart java-backend

# 重建并重启（代码更新后）
docker compose up -d --build java-backend

# 重启所有
docker compose restart
```

### 数据备份

```bash
# 备份 MySQL
docker exec multimodal-mysql mysqldump -uroot -p knowledge_management > backup_mysql.sql

# 备份 PostgreSQL
docker exec multimodal-pgvector pg_dump -U user vector_db > backup_pg.sql

# 恢复 MySQL
docker exec -i multimodal-mysql mysql -uroot -p knowledge_management < backup_mysql.sql

# 恢复 PostgreSQL
docker exec -i multimodal-pgvector psql -U user vector_db < backup_pg.sql
```

### 清理

```bash
# 停止并删除所有容器（数据卷保留）
docker compose down

# 停止并删除所有容器 + 数据卷（危险！会丢失所有数据）
docker compose down -v

# 清理无用的 Docker 镜像
docker image prune -f
```

---

## 四、常见问题

### Q: 启动后前端页面 502

**原因**：后端还没启动完成（Java 编译和模型加载需要时间）。

**解决**：等待 1-3 分钟，然后刷新页面。查看后端是否 healthy：

```bash
docker compose ps
```

### Q: Python AI 服务启动很慢

**原因**：首次需要下载模型文件（约 1-2GB）。

**查看进度**：

```bash
docker compose logs -f python-service
```

看到类似 `Model loaded successfully` 的日志说明模型加载完成。后续启动会使用 Docker volume 缓存，不需要重新下载。

### Q: 上传文件报 413 Request Entity Too Large

**原因**：Nginx 默认上传限制 1MB，本项目已配置为 200MB。如果你自己修改了 nginx.conf，确认 `client_max_body_size` 设置正确。

### Q: 数据库连接失败

**检查步骤**：

```bash
# 1. 确认数据库容器是否 healthy
docker compose ps

# 2. 检查 .env 中的密码是否与 docker-compose.yml 一致
#    docker-compose.yml 通过 ${MYSQL_ROOT_PASSWORD} 引用 .env

# 3. 尝试手动连接
docker exec -it multimodal-mysql mysql -uroot -p
```

### Q: 如何查看 Redis 是否正常工作

```bash
# 进入 Redis 容器
docker exec -it multimodal-redis redis-cli

# 如果设置了密码
docker exec -it multimodal-redis redis-cli -a 你的密码

# 测试
PING
# 应返回 PONG

# 查看浏览计数缓冲
KEYS doc:view:*
```

### Q: ECS 部署后外网无法访问

**检查清单**：

1. 阿里云安全组是否开放 80 端口（入方向）
2. ECS 防火墙是否放行 80 端口：`firewall-cmd --add-port=80/tcp --permanent && firewall-cmd --reload`
3. Docker 容器是否正常运行：`docker compose ps`

### Q: 如何修改端口

编辑 `.env` 文件：

```bash
FRONTEND_PORT=8888   # 前端端口改为 8888
MYSQL_PORT=3308      # MySQL 暴露端口（默认 3308 避免冲突）
```

修改后重启：`docker compose up -d`

---

## 文件结构速查

```
multimodal-rag-system/
├── docker-compose.yml              # 主编排文件
├── deploy/
│   ├── DEPLOY.md                   # 本文件（部署指南）
│   ├── .env.example                # 环境变量模板
│   ├── nginx.conf                  # Nginx 配置（反向代理 + SPA）
│   ├── docker-compose.ecs.yml      # ECS 覆盖层（用 ACR 镜像）
│   ├── push-to-acr.sh              # 推送镜像到 ACR
│   └── ecs-deploy.sh               # ECS 一键部署
├── backend/
│   ├── Dockerfile                  # Java 多阶段构建
│   └── .dockerignore
├── frontend/
│   ├── Dockerfile                  # Vue → Nginx 多阶段构建
│   └── .dockerignore
└── python-services/
    └── Dockerfile                  # Python + 模型
```