# 电池全生命周期溯源系统

仓库包含三部分：

- `frontend/`：Vue 3 + Vite + Element Plus 的前端管理系统
- `backend/`：Spring Boot + MyBatis-Plus 的后端 API 服务（MySQL 持久化）
- `hardhat/`：Hardhat 本地区块链与示例合约（EvidenceRegistry）

---

## 1. 总体架构与数据流

### 1.1 三端关系

```text
┌──────────────┐        HTTP(JSON/Multipart)         ┌──────────────┐
│  Frontend    │  ───────────────────────────────▶   │   Backend     │
│  Vue + Vite  │                                      │ Spring Boot   │
└──────────────┘                                      └──────┬───────┘
                                                                │
                                                                │ MyBatis-Plus
                                                                ▼
                                                           ┌─────────┐
                                                           │  MySQL  │
                                                           └─────────┘
                                                                │
                                                                │ Web3j(JSON-RPC)
                                                                ▼
                                                           ┌─────────┐
                                                           │ EVM链   │
                                                           │ Hardhat │
                                                           └─────────┘
```

### 1.2 “上链存证”的最小化设计

链上合约 `EvidenceRegistry` 只做一件事：发出事件 `EvidenceRecorded(sender, methodName, paramsHash, timestamp)`。

- 方法名：业务动作标识（如 `submitRecyclingApply`、`saveRecyclingValuation`）
- 参数哈希：对参数 JSON 的 `keccak256` 哈希（不把明文业务数据写入链上，降低隐私暴露与链上成本）

合约：`hardhat/contracts/EvidenceRegistry.sol`

后端上链入口：`backend/src/main/java/com/bishe/service/impl/ChainTransactionServiceImpl.java`

---

## 2. 模块划分（后端）

后端采用典型的分层结构：

- `controller/`：HTTP API（鉴权、参数校验、编排业务流程）
- `service/` + `service/impl/`：业务逻辑与对外能力（如上链、审计回调）
- `mapper/` + `resources/mapper/`：MyBatis-Plus Mapper 与 SQL
- `entity/`：与数据库表对应的实体模型
- `interceptor/`：JWT 拦截器
- `config/`：CORS、静态资源映射
- `common/`：统一返回体与全局异常处理

### 2.1 系统管理模块（System）

对应入口：`backend/src/main/java/com/bishe/controller/SystemController.java`

核心能力：

- 登录/注册：`/system/user/login`、`/system/user/register`
- 获取当前用户信息（含 roles / permissions）：`/system/user/info`
- 用户/角色/部门/菜单管理（管理员限定）

鉴权机制：

- 登录成功返回 JWT Token
- 前端在 `Authorization: Bearer <token>` 中携带
- 后端通过 `JwtInterceptor` 解析 token 并将 `userId` 注入 request

相关文件：

- JWT：`backend/src/main/java/com/bishe/utils/JwtUtils.java`
- 拦截器：`backend/src/main/java/com/bishe/interceptor/JwtInterceptor.java`
- 拦截配置与放行接口：`backend/src/main/java/com/bishe/config/WebConfig.java`

### 2.2 电池业务模块（Battery）

对应入口：`backend/src/main/java/com/bishe/controller/BatteryBusinessController.java`

模块覆盖：

- 电池信息（`battery_info`）
- 生产批次（`production_batch`）
- 出厂质检（`quality_inspection`）
- 维修保养（`maintenance_record`）
- 销售管理（`sales_record`）

典型流程：

1) 生产/入库：录入电池与批次
2) 批次结束：将批次下“生产中”的电池置为“待质检”（`/battery/batch/end/{batchId}`）
3) 质检审核：生成审核任务（SysAudit），审核通过/驳回并回写业务表
4) 销售：提交销售记录，走审核流，审核通过更新电池状态，并上链记录
5) 维修：创建工单/记录、上传材料、完成工单并可上链

### 2.3 溯源与生命周期模块（Trace）

对应入口：`backend/src/main/java/com/bishe/controller/TraceabilityController.java`

模块覆盖：

- 流转记录（`battery_transfer_record`）
- 上链交易列表（`chain_transaction`）
- 回收流程（`recycling_appraisal`）
- 维修记录查询（与 `battery` 模块相互补充）
- 车辆信息（`vehicle_info`，用于“车-电绑定”与溯源展示）

关键点：

- 该模块更偏“跨业务聚合/查询 + 回收流程编排”
- 多处会调用 `chainService.submitTransaction(method, jsonParams)` 写入链上事件

### 2.4 审核中心模块（Audit）

对应入口：

- `backend/src/main/java/com/bishe/controller/AuditController.java`（审核列表、处理接口）
- `backend/src/main/java/com/bishe/service/impl/SysAuditServiceImpl.java`（审核状态回调写回业务表）

设计要点：

- `sys_audit` 作为“统一审核任务表”，以 `businessType + businessId` 关联不同业务
- 业务提交审核任务后，审核通过/驳回时由 `SysAuditServiceImpl#doAudit` 统一回调更新目标业务表

---

## 3. 模块划分（前端）

前端路由清单在：`frontend/src/router/index.js`，按角色（roles）控制可见与可访问页面。

### 3.1 通用基础设施

- 请求封装：`frontend/src/utils/request.js`（Axios，统一错误处理与 token 注入）
- 登录态：`frontend/src/store/user.js`（保存 token、roles、user 信息）
- 权限路由：`frontend/src/router/permission.js`（登录校验与动态路由注入）

### 3.2 业务页面模块

- 系统管理：`frontend/src/views/system/*`
- 电池管理：`frontend/src/views/battery/*`
- 渠道销售（车电绑定/流转）：`frontend/src/views/sales/*`
- 售后服务：`frontend/src/views/maintenance/*`
- 回收利用：`frontend/src/views/recycling/appraisal/index.vue`
- 溯源查询：`frontend/src/views/trace/index.vue`

与后端接口的对应关系：

- `frontend/src/api/battery.js` → `/battery/**`、`/audit/**`
- `frontend/src/api/trace.js` → `/trace/**`
- `frontend/src/api/system.js` → `/system/**`

---

## 4. 关键业务流程（可直接写论文的“逻辑链路”）

下面按“论文写作友好”的方式，把每条主流程拆成：参与模块 → 数据表 → 链上存证点。

### 4.1 生产与质检（生产商）

参与模块：

- 前端：电池信息、生产批次、出厂质检页面
- 后端：`BatteryBusinessController` + `SysAuditServiceImpl`

核心数据表：

- `production_batch`、`battery_info`、`quality_inspection`、`sys_audit`

逻辑：

1) 生产商录入批次与电池（形成 `battery_info`）
2) 批次结束：将电池状态更新为“待质检”
3) 质检记录提交后进入审核流（`sys_audit`）
4) 审核通过后回写 `quality_inspection`，并同步更新 `battery_info.status`

链上存证点：

- 质检记录保存/审核等关键动作会写入 `chain_transaction`（方法名 + 参数哈希）

### 4.2 销售与渠道流转（销售/4S店）

参与模块：

- 前端：电池销售、渠道销售（流转记录）
- 后端：`BatteryBusinessController`、`TraceabilityController`、`SysAuditServiceImpl`

核心数据表：

- `sales_record`、`battery_transfer_record`、`sys_audit`

逻辑：

1) 创建销售记录 → 提交审核
2) 审核通过 → 回写销售状态，并更新电池状态（进入“已销售/使用”等）
3) 需要时记录“转出方/接收方”的流转（可用于溯源链路展示）

链上存证点：

- 销售审核通过时会记录链上交易（例如 `recordSales`）

### 4.3 维修与售后（维修站）

参与模块：

- 前端：售后工单、维修记录、材料上传
- 后端：`BatteryBusinessController`（维修工单与材料）

核心数据表：

- `maintenance_record`

逻辑：

1) 维修站创建/更新工单
2) 上传材料（文件落地到 `uploads/`，通过 `/files/**` 对外访问）
3) 完成工单后写入维修结论，必要时上链存证

### 4.4 回收利用（回收商/管理员）

参与模块：

- 前端：回收流程页面（申请 → 审核 → 上传照片/报告 → 估值 → 确认最终价 → 单据）
- 后端：`TraceabilityController` + `SysAuditServiceImpl`

核心数据表：

- `recycling_appraisal`、`sys_audit`、`chain_transaction`

关键逻辑（按时间线）：

1) 发起回收申请：创建 `recycling_appraisal`，并生成审核任务 `sys_audit`，同时上链存证申请
2) 审核：通过后生成 `recycleNo`，并更新电池状态为“回收中”
3) 上传材料：至少 3 张外观照 + 必传性能检测报告（文件可被 `/files/**` 访问），并上链存证
4) 自动估值：后端根据容量、状态、维修次数、流转次数计算预估价并落库（避免前端篡改与公式暴露）
5) 确认最终价：落库最终回收价并生成回收单据哈希（receipt），流程结束

链上存证点（示例）：

- `submitRecyclingApply`
- `auditRecyclingApply`
- `uploadRecyclingPhoto` / `uploadRecyclingReport`
- `saveRecyclingValuation`
- `confirmRecyclingPrice`
- `generateRecyclingReceipt`

### 4.5 溯源查询（多角色）

参与模块：

- 前端：溯源查询页
- 后端：`TraceabilityController`（链上交易列表、业务记录聚合）

核心数据：

- 业务表（电池/质检/维修/回收/销售/流转） + `chain_transaction`

逻辑：

- 以 `batteryId` 为主键串联各阶段业务记录
- 以 `chain_transaction.methodName` 与时间序列展示“链上证据”

---

## 5. 数据库与实体映射

完整表结构说明见：`DATABASE_SCHEMA.md`。

实体类目录：`backend/src/main/java/com/bishe/entity/`。

论文写作建议：

- 建议在论文中用 ER 图表达“主实体 battery_info 与 质检/维修/回收/销售/流转 的一对多关系”
- 重点解释 `sys_audit` 的“统一审核中台”设计：让不同业务复用同一套审核流程与回调逻辑

---

## 6. 文件存储与访问（上传材料）

上传文件会落地到后端工作目录下的 `uploads/`（例如回收照片、检测报告、销售材料、维修材料等）。

- 后端通过 `WebConfig#addResourceHandlers` 将 `uploads/` 映射为 `/files/**` 静态资源
- 前端保存的是 URL（如 `/files/recycling/photos/{appraisalId}/{file}`）

---

## 7. 本地运行

### 7.1 环境要求

- Node.js（用于前端与 hardhat）
- Maven + JDK 19（后端 `pom.xml` 指定 Java 19）
- MySQL 8.x

### 7.2 启动顺序

1) MySQL：导入/创建数据库 `bishe`（表结构可参考 `DATABASE_SCHEMA.md`）
2) Hardhat（可选，上链存证）：

```bash
cd hardhat
npm install
npx hardhat node
```

另开终端部署合约：

```bash
cd hardhat
npm run deploy
```

3) 后端：

```bash
cd backend
mvn -DskipTests spring-boot:run
```

4) 前端：

```bash
cd frontend
npm install
npm run dev
```

默认访问：

- 前端：http://localhost:5173/
- 后端：http://localhost:8084/

### 7.3 默认账号

默认账号来自当前数据库 `sys_user`，用于演示不同角色的业务权限。

| 用户名        | 密码   | role_key     | 角色身份（业务含义） | 主要权限范围（前端页面/后端接口）                                             |
| ------------- | ------ | ------------ | -------------------- | ----------------------------------------------------------------------------- |
| admin         | admin  | admin        | 系统管理员           | 全部模块：系统管理、生产质检、销售、售后、回收、溯源、审核中心                |
| catl          | 123456 | manufacturer | 生产商操作员         | 生产与质检：电池信息、生产批次、出厂质检、查看溯源                            |
| tesla_sh      | 123456 | dealer       | 渠道/4S店操作员      | 渠道销售：车电绑定、流转记录、查看溯源；可查看销售相关列表                    |
| fix_station   | 123456 | maintenance  | 维修站操作员         | 售后服务：售后工单/维修记录、材料上传；可发起回收申请并上传回收材料；查看溯源 |
| brunp         | 123456 | recycler     | 回收商操作员         | 回收流程：审核回收申请、上传材料、自动估值、确认最终价、开具单据；查看溯源    |
| recycler_test | 123456 | recycler     | 回收商测试账号       | 同 recycler（用于测试回收全流程）                                             |
| user01        | 123456 | owner        | 车主/终端用户        | 溯源查询：按电池ID查看生命周期记录与链上存证                                  |
| user02        | 123456 | owner        | 车主/终端用户        | 同 owner（溯源查询）                                                          |
| user03        | 123456 | owner        | 车主/终端用户        | 同 owner（溯源查询）                                                          |

权限说明（role_key → 访问边界）：

- admin：不做业务限制，拥有系统全部权限
- manufacturer：面向生产与出厂质检流程
- dealer：面向渠道销售与车电绑定/流转
- maintenance：面向售后工单与维修材料（并可配合回收流程提交材料）
- recycler：面向回收审核、估值与最终定价
- owner：仅用于溯源查询与证据查看
