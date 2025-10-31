# Docker & Kubernetes 学习速成与面试手册

> 目标：让初学者也能说清楚“是什么 / 为什么 / 怎么做”，并在面试场景下有理有据。

---

## 1. 基础概念三连

| 关键词 | 一句话解释 | 重点记忆 |
|--------|-------------|----------|
| **镜像 (Image)** | 带有运行环境和程序的一堆“只读层” | 分层结构 + 缓存机制 |
| **容器 (Container)** | 镜像跑起来再加一层可写层的进程 | 写时复制 (Copy-on-Write) |
| **UnionFS** | 把多层目录合并成一个视图 | Docker 依赖它组合镜像层 |
| **多阶段构建** | 构建层和运行层分开写 | 减小体积、更安全 |
| **docker-compose** | 多容器编排文件 | 服务依赖/网络/Volume 一键配置 |
| **Kubernetes** | “容器操作系统”，跨机器调度 | Pod/Deployment/Service/ConfigMap 等 |

---

## 2. 后端容器化：逐行拆解

`backend/Dockerfile`
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build    # 1) 构建层：带 Maven/JDK
WORKDIR /workspace
COPY mvnw pom.xml .mvn ./                      # 2) 先复制依赖相关文件，利于缓存
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B clean package -DskipTests     # 3) 编译出 app.jar

FROM eclipse-temurin:17-jre-alpine AS runtime  # 4) 运行层：只有 JRE
ENV JAVA_OPTS="" SPRING_PROFILES_ACTIVE=default
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**解释**：
1. **为什么多阶段？** 构建层包含 Maven 等大体积工具；运行层只保留 JRE + JAR，镜像变小、攻击面更小。
2. **依赖缓存**：先 COPY `pom.xml` + `.mvn`，只在依赖变化时重新下载，节省构建时间。
3. **健康检查**：`HEALTHCHECK` 让 Docker/K8s 能自动探测实例是否存活，抢救或摘除故障容器。
4. **环境变量注入**：`SPRING_*`、`MINIO_*` 等可由 compose/K8s 提供，避免硬编码配置。

**常见问答**：
- *问*：“多阶段构建有什么意义？”  
  *答*：最终镜像只包含运行必需文件，体积小（举例：几百 MB → 百来 MB），减少漏洞面；同时构建阶段和运行阶段职责明确。
- *问*：“如何把配置注入容器？”  
  *答*：环境变量（Compose `.env`、K8s ConfigMap/Secret），`application.yml` 用 `${ENV_VAR}` 读取；敏感信息用 Secret 管理。
- *问*：“健康检查需要注意什么？”  
  *答*：选择轻量接口（如 `/actuator/health`），设置合理的 interval/timeout/start-period，避免误判。

---

## 3. 前端容器化：Vite + nginx

`frontend/Dockerfile`
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci                             # 锁定依赖，适合 CI 环境
COPY . .
RUN npm run build                      # 输出 dist/

FROM nginx:1.25-alpine AS runtime
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

`frontend/deploy/nginx.conf`
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    location / {
        try_files $uri $uri/ /index.html;    # 前端路由 history 模式
    }

    location /api/ {
        proxy_pass http://backend:8080/api/; # 容器内直接转发到后端
    }
}
```

**面试要点**：
- *为何用 nginx？* 构建与运行分离、静态资源性能好、自带 gzip/缓存、方便做反向代理。
- *如何支持前端路由？* `try_files` 回退到 `index.html`。
- *前端如何访问后端？* 通过容器内 DNS（`backend:8080`），避免跨域。

---

## 4. docker-compose 编排思路

### 4.1 文件结构（节选）
```yaml
services:
  backend:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/campus_market...
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      MINIO_ENDPOINT: http://minio:9000
    depends_on:
      - mysql
      - redis
      - kafka
      - minio
  frontend:
    build: ./frontend
    depends_on:
      - backend
    ports:
      - "5173:80"
  mysql:
    image: mysql:8.0.33
    volumes:
      - mysql-data:/var/lib/mysql
      - ./scripts/db-init:/docker-entrypoint-initdb.d:ro
    environment:
      MYSQL_DATABASE: campus_market
      MYSQL_USER: campus_user
      MYSQL_PASSWORD: campus_pass
  # redis / kafka / minio / prometheus / grafana 略

volumes:
  mysql-data:
  redis-data:
  kafka-data:
  minio-data:
  grafana-data:
```

### 4.2 为什么这么写
- **自定义网络**：Compose 会自动创建网络，容器名即 DNS（例如 `mysql:3306`），无需写死 IP。
- **depends_on + healthcheck**：确保数据库、缓存、消息队列先就绪，后端再启动。
- **Volume**：`mysql-data` 等保证容器重启不丢数据；`scripts/db-init/init.sql` 负责第一次启动的建库建表。
- **环境变量管理**：通过 Compose 或 `.env` 注入数据库/Redis/MinIO/JWT 配置，避免硬编码。

### 4.3 需要答得上的问题
- “容器之间如何通信？” → 同一网络下用容器名；Compose 默认创建 bridge 网络。
- “数据怎么持久化？” → 挂载 volume，并且有定期备份脚本（`mysqldump`、`redis-cli --rdb`、`mc mirror`）。
- “如何一键启动/关闭？” → `scripts/docker-up.sh`（build + up -d）、`scripts/docker-down.sh`。

---

## 5. 数据与备份策略

| 组件 | 数据卷 | 备份方式 | 说明 |
|------|--------|----------|------|
| MySQL | `mysql-data` | `mysqldump` + binlog | `scripts/db-init` 初始化；备份文件可传 OSS |
| Redis | `redis-data` + AOF | 定期 `redis-cli --rdb` 或复制 AOF | 宕机时回放 AOF/RDB |
| MinIO | `minio-data` | `mc mirror` 或生命周期复制 | 存储对象文件，需做跨桶备份 |
| Kafka | `kafka-data` | 保留主题配置、消息回放脚本 | 保证重建主题、恢复消息顺序 |

**面试问法**：“容器删了数据怎么办？”  
回答要点：使用 Volume；定期备份；记录恢复 SOP；演练过恢复流程。

---

## 6. 故障排查速查表

| 场景 | 排查命令 | 说明 |
|------|----------|------|
| 容器起不来 | `docker compose ps`、`docker logs <服务>` | 先看日志，再看是否配置错误 |
| 端口占用 | `netstat -ano` | 调整映射或释放端口 |
| 健康检查失败 | `docker inspect --format='{{json .State.Health}}' <服务>` | 查看失败原因，确认探针 URL |
| 网络不通 | `docker exec -it <容器> ping mysql` | 验证容器间 DNS/网络 |
| 数据丢失 | 检查 `volumes` 配置、备份脚本 | 确保挂载目录正确，且有备份可恢复 |

---

## 7. Kubernetes 初阶答题要点

1. **核心资源**：Pod（最小部署单元）、Deployment（声明副本 + 滚动更新）、Service（负载均衡/服务发现）、ConfigMap/Secret（配置与密钥）。
2. **探针**：liveness（自愈）、readiness（流量切换）、startup（慢启动）；参数（initialDelay、period、timeout）。
3. **扩缩容**：`kubectl scale`、HPA（基于 CPU/自定义指标），配合资源 request/limit。
4. **持久化**：PVC + StorageClass，StatefulSet 用于有状态服务；外接云存储或 NFS。
5. **滚动更新/回滚**：`kubectl rollout status|history|undo`；解释版本控制机制。
6. **安全**：RBAC 控制权限、NetworkPolicy 限制流量、PodSecurityPolicy（或 OPA/Gatekeeper）。
7. **调试命令**：`kubectl get/describe/logs/exec/port-forward/top`，通常用于排查 CrashLoopBackOff、Pending、ImagePullBackOff 等。
8. **部署方式**：Helm/Kustomize 模板化；GitOps（ArgoCD/FluxCD）实现持续部署。

---

## 8. 面试问答模板

| 问题 | 示例回答 |
|------|----------|
| 镜像和容器的区别？ | 镜像是只读模版，由多层组成；容器是在镜像上加一层可写层的运行实例，写操作通过 Copy-on-Write 完成。 |
| 为什么要多阶段构建？ | 构建层包含编译工具，运行层只保留产出和运行时依赖 → 镜像小、风险低、构建快速。 |
| 如何给容器注入配置？ | 通过环境变量或挂载配置文件；Compose `.env`、K8s ConfigMap/Secret，应用读取 `${ENV}`。 |
| 容器之间如何互相访问？ | Compose 默认同一 bridge 网络，直接使用容器名；K8s 中通过 Service DNS。 |
| 数据会不会丢？ | 所有状态组件都挂 Volume；MySQL/Redis/MinIO 还有备份脚本，灾难恢复有 SOP。 |
| Redis/MySQL/Kafka 宕机怎么办？ | 有健康检查 + 自动重启；Redis 有会话降级；MySQL/Kafka 有备份/回放；Prometheus 监控告警。 |
| 在 K8s 上如何滚动更新？ | 修改镜像版本，`kubectl apply`，Deployment 滚动发布；监控探针，失败时 `kubectl rollout undo`。 |
| 如何排查容器启动失败？ | `docker compose ps` → `docker logs` → 检查健康检查和 config；K8s 用 `kubectl describe/logs`。 |

---

## 9. 建议的练习顺序
1. **照着文档跑一次**：`scripts/docker-up.sh`，观察各容器状态、日志、端口。
2. **修改配置**：用 `.env` 改数据库密码，验证容器能读取新配置。
3. **模拟故障**：停止 Redis，看后端是否进入 JWT 降级；停 MySQL 观察健康检查失败。
4. **备份与恢复演练**：跑 `mysqldump`、删除容器、重新启动恢复数据。
5. **遇到问题写成 Q&A**：积累自己的排查经验，面试时能举例说明。

掌握以上内容，你就能既说明白“为什么这么做”，也能对面试官的追问给出有深度的回答。祝你面试顺利！
