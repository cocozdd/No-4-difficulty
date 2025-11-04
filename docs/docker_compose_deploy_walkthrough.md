# Campus Market – Docker Compose Deployment Runbook

> 适用于面试演示或在自有 ECS 上快速复现环境。以下步骤默认服务器为 CentOS/AlmaLinux8 系，已经具备公网访问能力。命令中若出现占位符，请替换为实际值。

## 1. 服务器初始化
- 更新系统并安装 Docker 及 compose 插件
  ```bash
  sudo yum update -y
  sudo yum install -y yum-utils
  sudo yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
  sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  sudo systemctl enable --now docker
  sudo usermod -aG docker $USER && newgrp docker
  ```
- 配置国内镜像源缓解拉取失败
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
- 准备持久化目录并开放端口（安全组亦需同步放行）
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
- 克隆仓库
  ```bash
  cd /srv
  git clone https://github.com/cocozdd/No-4-difficulty.git campus-market
  cd /srv/campus-market
  ```
- 写入凭据（可根据面试强调安全性的需要说明这些值来自随机生成）
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
- 低内存机器可在面试时说明两种策略：
  1. 使用云数据库（RDS）并注释掉 Compose 内的 `mysql` 服务。
  2. 保留单机 MySQL，但在 `command` 中调低 `innodb_buffer_pool_size`、开 Swap。

## 3. 构建镜像
- 确保构建脚本可执行
  ```bash
  chmod +x backend/mvnw
  ```
- 构建后端与前端镜像（若网络受限，可先 `docker pull docker/dockerfile:1.4`）
  ```bash
  docker compose build
  ```
- 确认镜像存在
  ```bash
  docker images | grep campus-market
  ```

## 4. 依赖镜像拉取策略
- 若直接 `docker compose up` 因网络失败，可先通过国内镜像仓库手动拉取并打标签，例如：
  ```bash
  docker pull registry.cn-hangzhou.aliyuncs.com/bitnami/kafka:3.7
  docker tag registry.cn-hangzhou.aliyuncs.com/bitnami/kafka:3.7 bitnami/kafka:3.7
  docker rmi registry.cn-hangzhou.aliyuncs.com/bitnami/kafka:3.7
  ```
- Prometheus、Grafana、MinIO、MySQL、Redis 同理，将镜像保存为本地层后，启动时使用 `docker compose up -d --pull never`。
- 面试时可以强调：通过镜像代理/私有仓库提高成功率，并说明为什么坚持使用 `bitnami/kafka:3.7`（与 Spring Boot 2.7 自带客户端完全兼容）。

## 5. 启动与初始化
- 启动所有服务
  ```bash
  docker compose up -d --pull never
  docker compose ps
  ```
- 导入完整数据库结构（首次部署或重建时执行）
  ```bash
  docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" campus_market' < backup/campus_market_full.sql
  ```
- 验证表是否存在
  ```bash
  docker compose exec mysql sh -c 'mysql -ucampus_app -p"$MYSQL_PASSWORD" -e "SHOW TABLES;" campus_market'
  ```
- 若后端因 MySQL 未就绪出现连接失败，简单 `docker compose restart backend`。

## 6. 验证
- 后端日志：`docker compose logs backend --tail 50`
- 健康检查（需实际公网 IP）：`curl http://<ECS_IP>:8080/actuator/health`（返回 401 表示服务已启用鉴权，可在 Spring Security 中为 `/actuator/health` 放行）
- 前端：浏览器访问 `http://<ECS_IP>:5173`
- MinIO 控制台：`http://<ECS_IP>:9090`，使用 `.env` 中的凭据
- Grafana：`http://<ECS_IP>:3000`（admin/admin）
- Prometheus：`http://<ECS_IP>:9091`
- 面试演示可说明：如何通过 `docker compose exec backend sh -c "apk add --no-cache mysql-client && mysql -hmysql ..."` 来现场确认依赖连通性。

## 7. 运维要点
- 保持数据卷备份：`/var/lib/docker/volumes/campus-market_mysql-data`、`..._minio-data` 等。
- 将 `docker compose up -d` 写入 systemd 单元，实现开机自启：
  ```ini
  # /etc/systemd/system/campus-market.service
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
- 定期运行 `docker compose logs --since 1h backend` 查看错误、`docker compose pull` 获取最新镜像并 `docker compose up -d backend` 进行滚动更新。
- 若要在面试中展示 DevOps 思维，可补充 CI/CD 思路：GitHub Actions 负责测试与镜像构建 → 推送至 ACR → ECS 上执行 `docker pull` + `docker compose up -d`。

---

**面对面试官的讲述重点**
1. **环境准备**：容器运行时、镜像加速、目录权限、安全组。
2. **配置管理**：使用 `.env` + override 统一凭据，强调安全性。
3. **镜像构建与依赖拉取**：处理网络问题的策略，说明坚持兼容版本的原因。
4. **数据库初始化**：自动化脚本来源、如何确认导入成功。
5. **验证步骤**：日志、健康检查、前端联调、监控入口。
6. **运维与扩展**：备份、自启、CI/CD 规划及未来迁移 k3s 的可能性。

在面试中按照此 runbook 讲述，可充分体现端到端 DevOps 思路与实操能力。
