# Campus Market Docker Compose 运维手册

本文整理我在阿里云 ECS 上部署校园二手交易平台的完整步骤，也总结了当前 CI/CD 流水线的成果，便于面试时展示端到端 DevOps 实践。

## 1. 服务器初始化
- **安装 Docker 与 compose 插件**
  ```bash
  sudo yum update -y
  sudo yum install -y yum-utils
  sudo yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
  sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  sudo systemctl enable --now docker
  sudo usermod -aG docker $USER && newgrp docker
  ```
- **配置镜像加速**
  ```bash
  sudo tee /etc/docker/daemon.json <<'EOF'
  {
    "registry-mirrors": [
      "https://registry.cn-hangzhou.aliyuncs.com",
      "https://docker.1ms.run/",
      "https://docker.xuanyuan.me/"
    ]
  }
  EOF
  sudo systemctl daemon-reload
  sudo systemctl restart docker
  ```
- **准备持久化目录与防火墙**
  ```bash
  sudo mkdir -p /srv/campus-market/{mysql-data,redis-data,kafka-data,minio-data,grafana-data}
  sudo chown -R $USER:$USER /srv/campus-market
  sudo firewall-cmd --add-port=80/tcp --add-port=443/tcp \
    --add-port=8080/tcp --add-port=5173/tcp --add-port=3000/tcp \
    --add-port=3306/tcp --add-port=6379/tcp --add-port=9000/tcp \
    --add-port=9090/tcp --add-port=9091/tcp --add-port=9092/tcp \
    --permanent && sudo firewall-cmd --reload
  ```

## 2. 代码与配置
- **克隆项目**
  ```bash
  cd /srv
  git clone https://github.com/cocozdd/No-4-difficulty.git campus-market
  cd campus-market
  ```
- **准备 `.env`（示例凭据，正式环境需替换）**
  ```bash
  cat <<'EOF' > .env
  POSTGRES_DB=campus_market
  POSTGRES_USER=campus_app
  POSTGRES_PASSWORD=Yf4m@9!cS7Lh21QP

  SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
  SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}

  JWT_SECRET=pS1mVn9#Dw3!kR5@tHg6zQ1cL7eB8yFp

  MINIO_ACCESS_KEY=campus_minio
  MINIO_SECRET_KEY=Se3#p1T8qWz9X6mK
  MINIO_BUCKET_GOODS=campus-market-goods
  MINIO_BUCKET_CHAT=campus-market-chat
  EOF
  chmod 600 .env
  ```
- **可选：接入外部 MinIO**
  - 如果已在宿主机或云端部署 MinIO，可结合仓库中的 `docker-compose.override.local-minio.yml` 覆盖文件：
    ```bash
    docker compose -f docker-compose.yml -f docker-compose.override.local-minio.yml up -d
    ```
    覆盖文件会禁用内置 MinIO 服务，并把后端的 `MINIO_ENDPOINT` 指向 `host.docker.internal:9000`（如需其他地址，修改文件或在命令行传入环境变量即可）。

## 3. 镜像构建
- 赋权 Maven Wrapper：`chmod +x backend/mvnw`
- 构建镜像：`docker compose build`
- 查询结果：`docker images | grep campus-market`

## 4. 依赖镜像策略
- 国内拉取困难时，先从阿里云镜像站拉取并重打标签：
  ```bash
  docker pull registry.cn-hangzhou.aliyuncs.com/bitnami/kafka:3.7
  docker tag registry.cn-hangzhou.aliyuncs.com/bitnami/kafka:3.7 bitnami/kafka:3.7
  docker rmi registry.cn-hangzhou.aliyuncs.com/bitnami/kafka:3.7
  ```
- Prometheus、Grafana、MinIO、PostgreSQL、Redis 同理，之后使用 `docker compose up -d --pull never` 启动。
- 面试时可强调：镜像镜像源 fallback、版本锁定、Kafka 3.7 与 Spring Boot 2.7 的兼容性。

## 5. 启动与数据库导入
- 启动所有容器：
  ```bash
  docker compose up -d --pull never
  docker compose ps
  ```
- 导入 schema 和样例数据（仓库默认的 `scripts/db-init/postgres-init.sql` 只建表，如需演示数据可自行补充 insert 脚本）：
  ```bash
  docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"
  # 需要额外导入演示数据时：
  # docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < backup/campus_market_demo.sql
  ```
- 若后端抢在 PostgreSQL 前启动，可执行 `docker compose restart backend`。

## 6. 验证清单
- 后端日志：`docker compose logs backend --tail 50`
- 健康检查：`curl http://<ECS_IP>:8080/actuator/health`（返回 401 说明安全策略生效，可视需要放行）
- 前端 UI：`http://<ECS_IP>:5173`
- MinIO 控制台：`http://<ECS_IP>:9090`
- Grafana：`http://<ECS_IP>:3000`
- Prometheus：`http://<ECS_IP>:9091`
- CLI 演示：
  ```bash
  docker compose exec backend sh -c "apk add --no-cache postgresql-client && psql -hpostgres -U ${SPRING_DATASOURCE_USERNAME} -d ${POSTGRES_DB} -c 'SELECT 1'"
  ```

## 7. 运营维护
- 定期备份数据卷（PostgreSQL、MinIO、Grafana 等）。
- 配置 systemd 自启动：
  ```ini
  [Unit]
  Description=Campus Market Stack
  After=docker.service
  Requires=docker.service

  [Service]
  WorkingDirectory=/srv/campus-market
  ExecStart=/usr/bin/docker compose up -d
  ExecStop=/usr/bin/docker compose down
  Restart=always

  [Install]
  WantedBy=multi-user.target
  ```
  ```bash
  sudo systemctl enable --now campus-market
  ```
- 日常巡检：`docker compose logs --since 1h backend`、`docker compose pull`、`docker compose up -d backend`。

## 8. CI/CD 自动化成果
- **工作流**：`.github/workflows/ci-backend.yml`，支持 `main/master` 推送、PR 与手动触发。
- **Secrets**：`ACR_SERVER`、`ACR_USERNAME`、`ACR_PASSWORD`、`ECS_SSH_HOST`、`ECS_SSH_USER`、`ECS_SSH_PORT`、`ECS_SSH_KEY`。
- **ECS 覆盖文件**：`docker-compose.override.acr.yml` 指向 `${ACR_SERVER}/campus-market/campus-market-backend:${IMAGE_TAG}`。
- **部署脚本** `/srv/campus-market/scripts/deploy-backend.sh`：
  1. 登录 ACR 并拉取指定标签镜像；
  2. 结合 `docker-compose.yml` 与覆盖文件重建后端；
  3. 对 `/actuator/health` 做至多 12 次健康检查，HTTP 200/401 均视为成功。
- **流水线阶段**：
  1. `test`：在 `backend` 目录执行 `./mvnw -B verify`；
  2. `build-and-push`：构建镜像并以 `{SHA}` 与 `latest` 标签推送到 ACR；
  3. `deploy`：加载 CI 专用 SSH Key，`ssh-keyscan` 写入 host key 后远程执行部署脚本。
- **关键实践**：覆盖文件解耦镜像地址、CI 专用密钥+`ssh-keyscan`、健康检查与安全策略对齐，可在面试时结合最新 Actions 运行记录展示。

## 9. 面试讲述要点
1. 环境准备：容器运行时、镜像加速、权限与防火墙。
2. 配置管理：`.env` + override、凭据轮换、外部服务替换方案。
3. 镜像策略：版本锁定、镜像代理、Kafka 3.7 选型。
4. 数据库生命周期：自动初始化与验证命令。
5. 可观测性：日志、健康检查、Grafana/Prometheus。
6. CI/CD 叙事：Secrets、流水线分层、部署脚本、成功案例。
7. 后续规划：前端流水线、Flyway 迁移、Terraform/Ansible。

## 10. CI/CD 总结与价值回顾
1. **流程目标**：实现“代码提交 → 自动化测试 → Docker 镜像构建 → 推送阿里云 ACR → 部署到 ECS”的端到端自动化，消除手工操作。
2. **关键执行步骤**：
   - 配置 ACR，录入 GitHub Secrets（ACR 与 ECS 凭据）；
   - 调试流水线过程中先后解决 Maven Wrapper、ACR 推送、SSH 登录、镜像拉取、健康检查等问题；
   - 部署脚本结合阿里云镜像加速和健康检查策略，确保上线稳定。
3. **最终成果**：`test → build-and-push → deploy` 三阶段全部自动化，当前流水线已连续成功运行。
4. **流程意义**：提交代码即可自动完成测试、构建、部署，无需手动登录服务器或镜像仓库，大幅缩短迭代周期，也为团队协作和持续交付打下基础。

通过以上步骤和总结，我可以向面试官清楚地说明我的 DevOps 实践能力：不仅能手工部署，还能把流程标准化、自动化，真正实现“写代码、推送、上线”的一体化闭环。

## 11. 最近问题与改进记录

| 时间 | 现象 | 根因 | 解决方案 | 后续改进 |
| --- | --- | --- | --- | --- |
| 2025-11：本地 IDEA 启动后端报 `No resolvable bootstrap urls` | `spring.kafka.bootstrap-servers` 默认仍是容器网络地址 `kafka:9092` | 本地环境解析不到 `kafka` 主机名 | 将 `backend/src/main/resources/application.yml` 默认改为 `localhost:9092`，容器继续用 `SPRING_KAFKA_BOOTSTRAP_SERVERS` 覆盖 | 规范：配置文件默认指向 localhost，部署环境一律靠环境变量/override 注入 |
| 2025-11：打包后的前端 WebSocket 连到 `ws://localhost:5173/ws` 失败 | `resolveEndpoint()` 只对 dev 端口做特殊处理，静态站点托管在 5173 时没有 WS | 生产部署缺少固定 WS 地址 | 新增 `VITE_WS_ENDPOINT`，在 `.env.production` 写 `ws://<backend>/ws`，或把前后端托管到同一域名 | 上线 checklist 加入「确认 VITE_WS_ENDPOINT/反向代理配置」 |
| 2025-11：图片上传成功但浏览器显示 `FAILED` | MinIO bucket 默认私有且生成 URL 为 `http://minio:9000/...` | 前端访问不到容器内网 Host，且没有公开读取策略 | `MinioProperties` 加 `publicEndpoint`、`MINIO_PUBLIC_ENDPOINT`；`MinioConfig` 自动下发只读策略 | 部署 checklist 增加「MINIO_PUBLIC_ENDPOINT 可访问」「bucket policy 允许匿名读取」 |
| 2025-11：Element Plus 控制台警告 `type.text is about to be deprecated` | 仍大量使用 `el-button type="text"` | Element Plus 3 将移除此类型 | 将按钮改为 `link`（`frontend/src/views/MyGoodsView.vue`、`frontend/src/views/CartView.vue` 等） | 前端 lint 规则里加入 Element Plus 的 breaking change 检查 |

**流程层面的改进建议**
1. **环境隔离**：保持 `application.yml`/`.env` 指向本地依赖，服务器/容器差异通过环境变量与 override 文件注入，避免 IDE 和 Docker 配置漂移。
2. **统一发布入口**：将 `docker compose build && docker compose up -d`（或 K8s apply）固化为脚本，服务器升级时执行同一步骤，便于排错和回滚。
3. **发布前检查表**：新增 Kafka、MinIO、WebSocket 连通性检查项，确保 `bootstrap.servers`、`MINIO_PUBLIC_ENDPOINT`、`VITE_WS_ENDPOINT` 等关键变量都指向可访问地址。
4. **旧版本并存策略**：服务器上若保留旧部署，先比对 `.env` / compose 覆盖文件再发布，必要时安排维护窗口做滚动重启，并记录镜像版本与回滚方式。
