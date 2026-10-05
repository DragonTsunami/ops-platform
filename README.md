# ops-platform — 真实开源软件生产化组装：网页运维平台

> 定位：**运维第二作品**。不写一行业务代码，把 4 个成熟开源组件组装成「网页里运维」的平台——
> 证明的不是「我会写软件」，而是「我能运维真实世界给的东西」。

## 为什么是这个组合

| 组件 | 版本 | 角色 | 一句话 |
|---|---|---|---|
| [Semaphore UI](https://github.com/semaphoreui/semaphore) | v2.19.12 | 操作面板 | Ansible playbook 网页执行：绑 Git 仓库自动拉取，点按钮跑任务 |
| [Uptime Kuma](https://github.com/louislam/uptime-kuma) | 2.5.5 | 可见性 | 拨测监控 + 公开状态页（HTTP/TCP/证书/推送） |
| [Homepage](https://github.com/gethomepage/homepage) | v2.4.0 | 门面 | 所有面板入口一页导航（配置即代码） |
| [Jenkins](https://www.jenkins.io/) | 2.580.1-lts | 构建版本 | 流水线即代码：JCasC 建 job，从 Gitea 拉 playbook 跑语法检查（logRotator 30/5） |
| [Gitea](https://gitea.io/) | 28.0.0 | 内容版本 | playbook 源仓本地托管（commit=版本，永远全留） |
| [Restic](https://github.com/restic/restic) | D3 接入 | 数据版本 | 增量+加密+去重，官方设计原则含 Verifiable（可验证恢复），keep_daily 7 |

底座：Prometheus + Grafana + Alertmanager（复用 [clinic-ops](https://github.com/DragonTsunami/clinic-ops) 已验证模板）。

## 目录结构

```
ops-platform/
├── docker-compose.yml      # 六件套+版本管理编排（端口 4000/4101/4002/4103/4104/4105，127.0.0.1 绑定）
├── .env.example            # 环境参数模板（真实值在 .env，不入库）
├── homepage/               # 仪表盘配置（settings/services/widgets，配置即代码）
├── jenkins/                # 构建层配置即代码（casc JCasC + init.groovy.d + plugins.txt 声明式插件）
├── kuma/monitors-seed.sql  # Kuma 11 监控 seed（密码 [CHANGE_ME] 占位；D5 重建用）
└── ansible/                # 平台自管 playbook（嵌套 git 仓，远端=本地 Gitea ops-ansible）
```

## 本地运行

```bash
cp .env.example .env        # 填真实值：openssl rand -base64 32 / openssl rand -hex 8
docker compose up -d
# Semaphore  → http://127.0.0.1:4000
# Kuma       → http://127.0.0.1:4101
# Homepage   → http://127.0.0.1:4002
# Adminer    → http://127.0.0.1:4103   # 数据库只读查看（admin 账号=MySQL 只读身份仅 SELECT；改库走 DBeaver root）
# Jenkins    → http://127.0.0.1:4104   # 构建版本层（ops-ansible-check job，H/5 轮询 Gitea）
# Gitea      → http://127.0.0.1:4105   # 内容版本层（ops-ansible 源仓）
# 状态页     → http://127.0.0.1:4101/status/ops   # Kuma 公开状态页（平台+业务两个分面）
```

## Kuma 拨测矩阵（配置即代码）

11 条监控定义在 [`kuma/monitors-seed.sql`](kuma/monitors-seed.sql)（SQLite 直插姿势，密码 `[CHANGE_ME]` 占位）：

- **平台面板组**：Homepage / Semaphore / Kuma 自身 / Adminer / Jenkins（拨 `/login` 绕登录墙）/ Gitea / 运维面板
- **业务监控组**：clinic Grafana / 前端 / MySQL（`SELECT 1` 探针）/ Redis（`PING` 探针）
- 重建姿势：容器起来后 `docker exec ops-kuma sqlite3 /app/data/kuma.db < kuma/monitors-seed.sql` → 回填密码 → `docker restart ops-kuma`
- 坑位账：MySQL/Redis 走 `database_connection_string`（URL 串），填 hostname 字段反而不生效；Jenkins 拨根路径 403（登录墙），拨 `/login` 200

## AI 协作方式

与 clinic-ops 相同纪律：AI 起草 → 人工读懂 → 网页执行留痕。
Semaphore 官方提供 MCP Server——AI 助手可直驱面板触发任务（本轮已核实，启用记录见 docs/）。
