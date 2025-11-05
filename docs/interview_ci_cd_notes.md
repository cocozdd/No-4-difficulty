# 面试准备清单（CI/CD 方案）

## 1. 流程设计：讲清“为什么这么搭”

- **目标**：提交代码后自动完成测试、构建与部署，避免人工操作带来的延迟和失误。
- **工具选择**：使用 GitHub Actions，原因是与代码仓库无缝集成、YAML 配置简单、免费额度覆盖个人项目场景，还能直接复用社区 Action（如 `actions/setup-java`、`docker/build-push-action`）。
- **阶段拆分**：流水线划分为 `test → build-and-push → deploy` 三段。测试失败立刻阻断后续流程，防止构建与部署浪费资源；阶段隔离后，若部署异常只需排查 SSH 或容器启动问题，无需重新跑测试，提高定位效率。
- **安全管理**：ACR 密码、ECS 私钥等敏感凭据存放于 GitHub Secrets，流水线执行时按需解密，避免写进仓库被泄露。

## 2. 问题解决：准备“坑怎么踩、怎么填”的故事

每个案例按照“现象 → 排查 → 解决 → 总结”整理，可在面试时讲述约 1–2 分钟。

| 问题类型 | 面试表述要点 |
| --- | --- |
| Maven Wrapper 缺失 | 流水线 `test` 阶段提示 `./mvnw: command not found`，对比后发现 `.mvn` 目录没提交；本地执行 `mvn wrapper:wrapper` 生成 Wrapper 并提交，规范是“建新项目先生成 Wrapper 再提交”。 |
| ECS SSH 连接失败 | `deploy` 阶段报 `SSH 连接超时`，先补齐安全组 22 端口，再检查 Secrets 中私钥格式（缺少 `-----BEGIN RSA PRIVATE KEY-----`）；修正后连接恢复，经验是“先查云端网络规则，再核对凭证格式”。 |
| Docker 拉取超时 | ECS 拉镜像卡在 `pulling image`，判断是国内访问 Docker Hub 慢；在 `/etc/docker/daemon.json` 配置阿里云加速器，重启 Docker 后拉取从几分钟降至几十秒，总结为“国内部署先配镜像源”。 |

## 3. 技术细节：答清关键配置

- **Dockerfile 核心指令**：`FROM openjdk:17-jdk-slim`（瘦身基础镜像）、`COPY target/*.jar app.jar`（复制打包产物）、`EXPOSE 8080`（开放服务端口）、`ENTRYPOINT ["java","-jar","app.jar"]`（保证 Java 进程为 1 号进程）。
- **ECS 细节**：系统 CentOS 7，SSH 使用默认 22 端口；部署命令链是 `docker stop` → `docker rm` → `docker pull $ACR_SERVER/...:$TAG` → `docker run -d -p 8080:8080 --name second-hand-api ...:$TAG`，用 `|| true` 忽略无容器时的报错。
- **健康检查**：Actuator 暴露 `health,info`，关闭认证避免 401；流水线健康检查重试间隔调成 5 秒、次数 3 次，匹配服务启动时间。

## 4. 成果价值：量化流程带来的改变

- **效率**：手动部署包含打包、上传、停旧容器、启动新容器约需 15 分钟；流水线自动执行后 2–3 分钟完成，而且无需盯守。
- **质量**：每次提交都会运行单元测试与代码检查，例如一次提交的 SQL 语法错误被 `test` 阶段拦截，避免带 bug 上线。
- **可复用**：`.github/workflows/*.yml` 改镜像名和端口即可迁移至其它 Spring Boot 项目，后续二开成本低。

## 避坑提醒

- **表述方式**：强调参考最佳实践并按校园二手平台需求适配，不要说“照着教程做”。
- **直面空白**：被问到“是否考虑回滚”可说明目前靠手动回滚旧镜像，企业版会在 `deploy` 后加自动回滚触发条件。
- **落地细节**：所有回答都落在具体配置，例如 ACR 建在“个人版私有仓库 second-hand-platform-backend”，镜像标签使用 GitHub commit 短 SHA 对应版本。

## 1 分钟项目介绍

> “我负责校园二手交易平台后端的 CI/CD，搭建了一条 GitHub Actions 流水线：代码提交后自动跑测试、构建镜像推到阿里云 ACR，再通过 SSH 部署到 ECS。过程中解决了 Maven Wrapper 缺失、ECS SSH 连接、Docker 拉取超时等问题。现在提交代码两分钟内就能自动上线，也能提前拦截测试失败，效率和质量都有明显提升。”

