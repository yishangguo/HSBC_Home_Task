# 银行交易管理系统 (Bank Transaction Management System)

## 项目概述

这是一个基于Spring Boot的银行交易管理系统，按照作业要求实现，提供完整的交易管理功能。系统采用内存数据库存储，支持缓存机制以及并发操作。

## 技术栈

### 后端
- **Java 17**: 最新的LTS版本
- **Spring Boot 3.2.0**: 系统框架
- **Spring Data JPA**: 数据访问层
- **H2 Database**: 内存数据库
- **Spring Cache**: 缓存管理
- **Maven**: 项目管理和构建

### 前端
- **HTML5**: 语义化标记
- **CSS3**: 样式
- **JavaScript ES6+**: 异步操作和DOM操作
- **响应式设计**: 适配移动端
- **智能表单**: 可根据业务规则动态显示/隐藏字段

### 部署和运维
- **Docker**: 容器化部署
- **Kubernetes**: 云原生部署
- **健康检查**: 应用监控
- **自动扩缩容**: 负载管理


## 架构设计

### 设计思路
- **分层架构**: Controller → Service → Repository，职责清晰
- **DTO分离**: 创建和更新使用不同DTO，避免校验冲突
- **不可变设计**: 关键业务字段创建后不可修改，保证数据一致性
- **自动生成**: 交易号码和日期由后端自动处理，减少前端错误
- **缓存分层**: 多级缓存策略，提升查询性能

### 分层架构
- **接口层**: `controller` 提供 REST API，进行参数校验与错误映射。
- **服务层**: `service` 封装业务逻辑、幂等校验与事务控制，暴露稳定接口。
- **数据访问层**: `repository` 通过 Spring Data JPA 与数据库交互，统一查询入口。
- **模型与DTO**: `model` 持久化对象，`dto` 用于输入输出解耦，避免泄漏内部结构。

### 健壮性设计
- **输入校验**: 服务层校验必填、金额范围、时间窗口与类型合法性；控制层进行基本校验与错误结果的返回。
- **异常治理**: 统一异常处理 `GlobalExceptionHandler`，归一化错误响应，避免异常穿透。
- **幂等保证**: 以 `reference` 作为业务幂等键，插入前做去重校验。`reference` 由后端自动生成（格式 `TXNyyyyMMddHHmmssSSSNNNN`），前端不再输入；`transactionDate` 默认当前时间。
- **事务一致性**: 服务层声明式事务，写操作失败自动回滚；读操作默认只读事务，策略可扩展。

### 不可变字段与更新策略
- 创建后不可变：`reference`（交易号码）、`accountNumber`、`amount`、`type`、`transactionDate`。
- 仅可更新：`description`、`notes`。
- 更新接口使用 `UpdateTransactionRequest`（仅包含 `description`、`notes`），避免不相关字段校验与更新。

### 缓存与性能
- **缓存分层**: 使用 Spring Cache 与 `ConcurrentMapCacheManager`，缓存命名空间：
  - `transactions`: 单笔与分页查询缓存
  - `recentTransactions`: 最近交易列表
  - `accountBalances`: 账户余额与计数
  - `metadata`: 元数据（如交易类型）
- **缓存策略**: 读多写少的查询使用 `@Cacheable`，变更操作统一 `@CacheEvict(allEntries = true)`，保证可见性与一致性。
- **分页优化**: 全部列表与条件检索采用分页，分页参数纳入缓存键，避免大结果集压力。
- **批量/搜索**: 提供关键词检索与组合条件查询，便于后续接入全文索引。

### 索引的使用
- 在生产数据库对以下字段建立索引：
  - **唯一索引**: `reference`
  - **普通索引**: `accountNumber`, `type`, `transactionDate`, `(amount)`
  - **组合索引（可选）**: `(accountNumber, transactionDate DESC)` 以覆盖常见按账户与时间排序场景
- 搜索字段（`description`）可按需接入全文检索（如 PostgreSQL GIN/Trigram 或外部 Elasticsearch）。

### 并发处理
- **线程池**: 使用 servlet 容器与数据库连接池容量配置匹配的线程池；对外 I/O 下游限流与超时设置。
- **事务与隔离级别**: 读写分离可扩展；热点写操作聚合与乐观并发控制（参考 `reference` 幂等键）。
- **缓存命中**: 通过元数据与热点读的缓存命中降低数据库压力；适时设置 TTL（如切换到 Caffeine/Redis）。
- **扩展性**: 在 Kubernetes 下可水平扩容副本，利用就绪/存活探针与 HPA 进行弹性伸缩。

## 测试策略与覆盖面

### 单元测试（Service/Repository）
- 覆盖业务校验（空值、负金额、未来时间、非法类型）。
- 覆盖幂等与重复引用异常，更新路径的冲突校验。
- 覆盖分页、搜索与统计逻辑；余额计算的边界值（空和零）。
- 元数据接口 `getTransactionTypes` 的正确性。
- 更新约束测试：仅 `description`、`notes` 可变，其余字段保持不变；更新使用 `UpdateTransactionRequest`。

### 集成测试（Controller + DB + Cache）
- 端到端场景：创建 → 查询 → 更新 → 删除。
- 分页与条件查询的响应结构与元数据。
- 统一异常响应格式与 HTTP 状态码约定。
- 缓存命中路径（可在后续引入可观测性断言）。
- 编辑接口仅校验 `UpdateTransactionRequest` 字段，确保不会因缺少金额/类型/账户而 400。

### 性能与并发测试
- 并发创建（忽略重复引用的预期失败），统计吞吐与延迟。
- 批量分页检索的耗时阈值校验。
- 热点缓存命中（元数据与列表）访问延迟要求。

## 未来扩展计划（Roadmap）

- **完善监控告警**: 接入 Micrometer + Prometheus + Grafana；对缓存命中率、慢查询、错误率建立看板与告警。
- **缓存升级**: 从本地 `ConcurrentMap` 平滑迁移到 `Caffeine` 或 `Redis`，增加 TTL/最大容量与分布式一致性策略。
- **数据库演进**: 从 H2 迁移到 PostgreSQL/MySQL；补齐表结构索引与迁移脚本（Flyway/Liquibase）。
- **搜索能力**: 针对描述字段启用全文索引或接入 Elasticsearch，支持更复杂的筛选与聚合。
- **安全与合规**: 可以增加鉴权鉴证（OAuth2/OpenID）、审计日志、数据脱敏与加密存储。
- **领域边界**: 拆分为 `交易`、`账户`、`对账` 等独立微服务，可以通过事件驱动（Kafka）实现最终一致性。
- **弹性与成本**: HPA 策略优化、资源配额、自动化容量测试，灰度与金丝雀发布。

## 当前项目结构

```
hsbc/
├── src/
│   ├── main/java/com/hsbc/transaction/
│   │   ├── controller/          # REST API控制器
│   │   ├── service/             # 业务逻辑层
│   │   ├── repository/          # 数据访问层
│   │   ├── model/               # 数据模型
│   │   ├── dto/                 # 数据传输对象
│   │   │   ├── TransactionRequest    # 创建交易DTO
│   │   │   ├── UpdateTransactionRequest # 更新交易DTO
│   │   │   └── TransactionResponse    # 交易响应DTO
│   │   ├── exception/           # 异常处理
│   │   └── config/              # 配置类
│   ├── main/resources/          # 配置文件和静态资源
│   └── test/                    # 测试代码
├── k8s/                         # Kubernetes配置
├── Dockerfile                   # Docker镜像配置
├── docker-compose.yml           # Docker Compose配置
├── pom.xml                      # Maven配置
├── README.md                    # 项目文档
├── start.sh                     # 启动脚本
```


## 已实现的设计

### API设计 
- ✅ **RESTful API**: 遵循REST设计原则
- ✅ **完整端点**: 覆盖所有CRUD操作
- ✅ **高级查询**: 多条件组合查询和搜索
- ✅ **分页支持**: 高效的分页和排序
- ✅ **错误处理**: 统一的异常处理机制

### 数据管理
- ✅ **内存数据库**: H2数据库配置和优化
- ✅ **数据模型**: 完整的实体设计和关系
- ✅ **数据验证**: 全面的输入验证和约束
- ✅ **数据初始化**: 测试数据自动加载
- ✅ **不可变约束**: 关键业务字段创建后不可修改

### 性能优化 
- ✅ **缓存机制**: Spring Cache集成
  - 缓存命名空间：transactions, recentTransactions, accountBalances, metadata
  - 智能缓存策略：读多写少，变更时统一清理
- ✅ **分页查询**: 数据库级别的分页优化
- ✅ **并发处理**: 线程安全的服务实现
- ✅ **压力测试**: 完整的性能测试套件
- ✅ **自动生成**: 交易号码生成算法优化，避免冲突

### 测试覆盖
- ✅ **单元测试**: 服务层完整测试
  - 覆盖业务校验、幂等性、更新约束等
- ✅ **集成测试**: API端点测试
  - 端到端场景、编辑接口仅校验更新DTO
- ✅ **性能测试**: 并发和压力测试
- ✅ **测试配置**: 独立的测试环境
- ✅ **约束测试**: 不可变字段保护验证

<img width="451" height="781" alt="image" src="https://github.com/user-attachments/assets/c7d3a1d2-79fb-4dd6-9372-2b7bd2e8011e" />


### 容器化部署
- ✅ **Docker支持**: 完整的Dockerfile
- ✅ **Docker Compose**: 本地开发环境
- ✅ **Kubernetes**: 生产部署配置
- ✅ **健康检查**: 应用监控和自愈

### Web界面
- ✅ **UI**: 响应式设计
- ✅ **完整功能**: 所有API功能的Web实现
- ✅ **用户体验**: 直观的操作流程
- ✅ **智能表单**: 创建时隐藏自动生成字段，编辑时置灰不可变字段


## 核心功能

### 交易管理
- ✅ **创建交易**: 完整的表单验证和业务逻辑
  - 交易号码自动生成（格式：TXNyyyyMMddHHmmssSSSNNNN）
  - 交易日期默认当前时间
  - 前端无需输入交易号码和日期
- ✅ **查询交易**: 支持多种查询方式
- ✅ **修改交易**: 智能编辑功能，仅允许修改描述和备注
  - 不可变字段：交易号码、账户号码、交易金额、交易类型、交易日期
  - 专用更新DTO：UpdateTransactionRequest，避免无关字段校验
- ✅ **删除交易**: 安全的删除操作和确认机制
- ✅ **交易列表**: 分页显示和排序功能

编辑约束：创建后仅允许修改“交易描述、备注”；“交易号码、账户号码、交易金额、交易类型、交易日期”等字段均不可修改。

### 高级查询
- ✅ 按账户号查询
- ✅ 按交易类型查询
- ✅ 按日期范围查询
- ✅ 按金额范围查询
- ✅ 关键词搜索
- ✅ 多条件组合查询

### 账户管理
- ✅ 账户余额计算
- ✅ 交易统计
- ✅ 账户交易历史

### 性能特性
- ✅ 缓存机制
- ✅ 分页查询
- ✅ 并发处理
- ✅ 压力测试支持

## 快速开始

### 环境要求
- Java 17+
- Maven 3.6+

### 一键启动
```bash
# 克隆项目
git clone https://github.com/yishangguo/HSBC_Home_Task.git
cd hsbc

# 启动应用
./start.sh
```

应用将在 `http://localhost:8080` 启动

### Docker 运行

1. **构建镜像**
```bash
docker build -t transaction-management .
```

2. **运行容器**
```bash
docker run -p 8080:8080 transaction-management
```

3. **使用 Docker Compose**
```bash
docker-compose up -d
```

### Kubernetes 部署

1. **应用部署配置**
```bash
kubectl apply -f k8s/deployment.yaml
```

2. **检查部署状态**
```bash
kubectl get pods
kubectl get services
```

### 访问地址
- **Web界面**: http://localhost:8080
- **API文档**: http://localhost:8080/api/v1/transactions
- **H2控制台**: http://localhost:8080/h2-console
- **健康检查**: http://localhost:8080/actuator/health

## API 文档

### 基础端点
- `POST /api/v1/transactions` - 创建交易
- `GET /api/v1/transactions/{id}` - 获取交易详情
- `PUT /api/v1/transactions/{id}` - 更新交易（仅允许修改描述与备注）
- `DELETE /api/v1/transactions/{id}` - 删除交易

### 查询端点
- `GET /api/v1/transactions` - 获取所有交易（分页）
- `GET /api/v1/transactions/account/{accountNumber}` - 按账户查询
- `GET /api/v1/transactions/type/{type}` - 按类型查询
- `GET /api/v1/transactions/search?keyword={keyword}` - 关键词搜索
- `GET /api/v1/transactions/criteria` - 多条件查询

### 统计端点
- `GET /api/v1/transactions/account/{accountNumber}/balance` - 账户余额
- `GET /api/v1/transactions/account/{accountNumber}/count` - 交易数量
- `GET /api/v1/transactions/recent` - 最近交易

### 示例请求

#### 创建交易
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "12345678",
    "amount": 1000.00,
    "type": "DEPOSIT",
    "description": "Initial deposit"
  }'
```
说明：
- 交易号码 `reference` 由后端自动生成（格式 `TXNyyyyMMddHHmmssSSSNNNN`），无需前端传入。
- 交易日期 `transactionDate` 默认使用当前时间，前端无需传入。

#### 查询交易
```bash
curl http://localhost:8080/api/v1/transactions?page=0&size=10
```

## 测试

### 单元测试
```bash
./mvnw test
```

### 集成测试
```bash
./mvnw verify
```

### 性能测试
```bash
./mvnw test -Dtest=TransactionPerformanceTest
```

## 配置

### 应用配置
主要配置文件：`src/main/resources/application.yml`

- 数据库配置
- 缓存配置
- 日志配置
- 监控端点

### 环境配置
- `dev` - 开发环境
- `test` - 测试环境
- `docker` - Docker环境
- `kubernetes` - Kubernetes环境

## 监控

### 健康检查
- `GET /actuator/health` - 应用健康状态
- `GET /actuator/info` - 应用信息
- `GET /actuator/metrics` - 性能指标

### H2 控制台
- 访问：`http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:transactiondb`
- 用户名: `sa`
- 密码: `password`

## 性能特性

### 缓存策略
- 交易查询缓存
- 账户余额缓存
- 最近交易缓存

### 分页优化
- 支持排序
- 可配置页面大小
- 性能优化的分页查询

### 并发处理
- 线程安全的服务层
- 数据库连接池
- 事务管理



## 部署

### 生产环境
1. 配置生产数据库
2. 调整JVM参数
3. 配置监控和日志
4. 设置健康检查

### 容器化
- Docker镜像优化
- 多阶段构建
- 安全配置
- 资源限制

### Kubernetes
- 自动扩缩容
- 健康检查
- 资源管理
- 服务发现

## 故障排除

### 常见问题
1. **端口占用**: 修改 `application.yml` 中的端口配置
2. **内存不足**: 调整JVM参数或Docker资源限制
3. **数据库连接**: 检查H2数据库配置

### 日志
- 应用日志: `logs/application.log`
- 错误日志: `logs/error.log`
- 性能日志: `logs/performance.log`

## 演示地址
- 使用腾讯云实例基于docker部署：http://43.139.236.195:8080

## 联系方式

- 项目维护者: [EasonGuo]
- 邮箱: [yishangguo@foxmail.com]
- 项目地址: [https://github.com/yishangguo/HSBC_Home_Task]

## 更新日志

### v1.0.0 (2025-09-03)
- 初始版本发布
