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
- **编写 `.env` 与 `docker-compose.override.yml`（示例凭据，正式环境需替换）**
  ```bash
  cat <<'EOF' > .env
  SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/campus_market?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
  SPRING_DATASOURCE_USERNAME=campus_app
  SPRING_DATASOURCE_PASSWORD=Yf4m@9!cS7Lh21QP

  MYSQL_ROOT_PASSWORD=Rx79t#qVbL41c/?F
  MYSQL_USER=campus_app
  MYSQL_PASSWORD=Yf4m@9!cS7Lh21QP

  JWT_SECRET=pS1mVn9#Dw3!kR5@tHg6zQ1cL7eB8yFp

  MINIO_ACCESS_KEY=campus_minio
  MINIO_SECRET_KEY=Se3#p1T8qWz9X6mK
  MINIO_BUCKET_GOODS=campus-market-goods
  MINIO_BUCKET_CHAT=campus-market-chat
  EOF

  cat <<'EOF' > docker-compose.override.yml
  services:
    backend:
      environment:
        SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
        SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
        SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
        JWT_SECRET: ${JWT_SECRET}
        MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
        MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
        MINIO_BUCKET_GOODS: ${MINIO_BUCKET_GOODS}
        MINIO_BUCKET_CHAT: ${MINIO_BUCKET_CHAT}
    mysql:
      environment:
        MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
        MYSQL_DATABASE: campus_market
        MYSQL_USER: ${MYSQL_USER}
        MYSQL_PASSWORD: ${MYSQL_PASSWORD}
  EOF
  chmod 600 .env
  ```
- **低内存机思路**：可换用云数据库，或保留内置 MySQL 并调小 `innodb_buffer_pool_size`、启用 Swap。

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
- Prometheus、Grafana、MinIO、MySQL、Redis 同理，之后使用 `docker compose up -d --pull never` 启动。
- 面试时可强调：镜像镜像源 fallback、版本锁定、Kafka 3.7 与 Spring Boot 2.7 的兼容性。

## 5. 启动与数据库导入
- 启动所有容器：
  ```bash
  docker compose up -d --pull never
  docker compose ps
  ```
- 导入 schema 和样例数据：
  ```bash
  docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" campus_market' < backup/campus_market_full.sql
  docker compose exec mysql sh -c 'mysql -ucampus_app -p"$MYSQL_PASSWORD" -e "SHOW TABLES;" campus_market'
  ```
- 若后端抢在 MySQL 前启动，可执行 `docker compose restart backend`。

## 6. 验证清单
- 后端日志：`docker compose logs backend --tail 50`
- 健康检查：`curl http://<ECS_IP>:8080/actuator/health`（返回 401 说明安全策略生效，可视需要放行）
- 前端 UI：`http://<ECS_IP>:5173`
- MinIO 控制台：`http://<ECS_IP>:9090`
- Grafana：`http://<ECS_IP>:3000`
- Prometheus：`http://<ECS_IP>:9091`
- CLI 演示：
  ```bash
  docker compose exec backend sh -c "apk add --no-cache mysql-client && mysql -hmysql -ucampus_app -p${SPRING_DATASOURCE_PASSWORD} -e 'SELECT 1'"
  ```

## 7. 运营维护
- 定期备份数据卷（MySQL、MinIO、Grafana 等）。
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
