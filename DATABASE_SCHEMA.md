# 数据库设计文档 (Database Schema)

## 1. 系统管理模块 (System Management)

### 1.1 `sys_user` (用户表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| user_id | BIGINT | 20 | 是 | 用户ID | 主键, 自增 |
| dept_id | BIGINT | 20 | 是 | 部门ID | 关联 sys_dept |
| username | VARCHAR | 50 | 是 | 用户账号 | 唯一 |
| password | VARCHAR | 100 | 是 | 密码 | 加密存储 |
| nickname | VARCHAR | 50 | 否 | 用户昵称 | |
| email | VARCHAR | 50 | 否 | 邮箱 | |
| phone | VARCHAR | 20 | 否 | 手机号码 | |
| status | INT | 1 | 是 | 帐号状态 | 0:正常, 1:停用 |
| create_time | DATETIME | | 是 | 创建时间 | |
| update_time | DATETIME | | 否 | 更新时间 | |
| chain_account | VARCHAR | 42 | 否 | 链上账户地址 | 关联区块链账户 |

### 1.2 `sys_role` (角色表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| role_id | BIGINT | 20 | 是 | 角色ID | 主键, 自增 |
| role_name | VARCHAR | 50 | 是 | 角色名称 | 如: 生产商, 4S店 |
| role_key | VARCHAR | 50 | 是 | 角色权限字符串 | 如: manufacturer, dealer |
| status | INT | 1 | 是 | 角色状态 | 0:正常, 1:停用 |
| create_time | DATETIME | | 是 | 创建时间 | |

### 1.3 `sys_menu` (菜单权限表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| menu_id | BIGINT | 20 | 是 | 菜单ID | 主键, 自增 |
| menu_name | VARCHAR | 50 | 是 | 菜单名称 | |
| parent_id | BIGINT | 20 | 是 | 父菜单ID | |
| order_num | INT | 4 | 否 | 显示顺序 | |
| path | VARCHAR | 200 | 否 | 路由地址 | Vue路由path |
| component | VARCHAR | 255 | 否 | 组件路径 | Vue组件路径 |
| perms | VARCHAR | 100 | 否 | 权限标识 | 如: battery:add |
| menu_type | CHAR | 1 | 是 | 菜单类型 | M:目录, C:菜单, F:按钮 |

### 1.4 `sys_user_role` (用户角色关联表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| user_id | BIGINT | 20 | 是 | 用户ID | 联合主键 |
| role_id | BIGINT | 20 | 是 | 角色ID | 联合主键 |

### 1.5 `sys_role_menu` (角色菜单关联表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| role_id | BIGINT | 20 | 是 | 角色ID | 联合主键 |
| menu_id | BIGINT | 20 | 是 | 菜单ID | 联合主键 |

### 1.6 `sys_dept` (部门/机构表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| dept_id | BIGINT | 20 | 是 | 部门ID | 主键, 自增 |
| parent_id | BIGINT | 20 | 是 | 父部门ID | |
| dept_name | VARCHAR | 50 | 是 | 部门名称 | 如: 宁德时代, 特斯拉上海 |
| order_num | INT | 4 | 否 | 显示顺序 | |
| leader | VARCHAR | 20 | 否 | 负责人 | |
| phone | VARCHAR | 20 | 否 | 联系电话 | |
| status | CHAR | 1 | 是 | 部门状态 | 0:正常, 1:停用 |

### 1.7 `sys_dict` (数据字典表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| dict_code | BIGINT | 20 | 是 | 字典编码 | 主键, 自增 |
| dict_label | VARCHAR | 100 | 是 | 字典标签 | 如: 三元锂 |
| dict_value | VARCHAR | 100 | 是 | 字典键值 | 如: 1 |
| dict_type | VARCHAR | 100 | 是 | 字典类型 | 如: battery_type |

---

## 2. 生产与销售模块 (Production & Sales)

### 2.1 `battery_info` (电池主表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| battery_id | VARCHAR | 64 | 是 | 电池ID | 主键, 对应BMS编码, 全局唯一 |
| batch_id | BIGINT | 20 | 否 | 生产批次ID | 关联 production_batch |
| manufacturer_id | BIGINT | 20 | 是 | 生产商ID | 关联 sys_dept |
| type_code | VARCHAR | 20 | 是 | 电池类型 | 字典: battery_type (三元锂/磷酸铁锂) |
| capacity | DECIMAL | 10,2 | 是 | 额定容量 | 单位: kWh |
| voltage | DECIMAL | 10,2 | 是 | 标称电压 | 单位: V |
| cathode_material | VARCHAR | 50 | 否 | 正极材料 | |
| produce_date | DATE | | 是 | 生产日期 | |
| create_time | DATETIME | | 是 | 录入时间 | |
| status | INT | 2 | 是 | 当前状态 | 1:生产, 2:销售, 3:使用, 4:维修, 5:回收 |

### 2.2 `production_batch` (生产批次表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| batch_id | BIGINT | 20 | 是 | 批次ID | 主键, 自增 |
| batch_no | VARCHAR | 50 | 是 | 批次编号 | |
| manufacturer_id | BIGINT | 20 | 是 | 生产商ID | 关联 sys_dept |
| produce_date | DATE | | 是 | 生产日期 | |
| quantity | INT | 11 | 是 | 计划生产数量 | |

### 2.3 `quality_inspection` (出厂质检表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| id | BIGINT | 20 | 是 | ID | 主键, 自增 |
| battery_id | VARCHAR | 64 | 是 | 电池ID | 关联 battery_info |
| ocv | DECIMAL | 10,4 | 是 | 开路电压 | 单位: V |
| acr | DECIMAL | 10,4 | 是 | 交流内阻 | 单位: mΩ |
| insulation_res | DECIMAL | 10,2 | 是 | 绝缘电阻 | 单位: MΩ |
| air_tightness | VARCHAR | 20 | 是 | 气密性 | Pass/Fail |
| inspector | VARCHAR | 50 | 是 | 质检员 | |
| check_time | DATETIME | | 是 | 检测时间 | |
| data_hash | VARCHAR | 66 | 是 | 数据哈希 | 上链凭证 |

---

## 3. 使用与维护模块 (Usage & Maintenance)

### 3.1 `vehicle_info` (车辆信息表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| vehicle_id | BIGINT | 20 | 是 | 车辆ID | 主键, 自增 |
| vin | VARCHAR | 17 | 是 | 车架号(VIN) | 唯一 |
| battery_id | VARCHAR | 64 | 是 | 电池ID | 关联 battery_info |
| brand | VARCHAR | 50 | 是 | 品牌 | |
| model | VARCHAR | 50 | 是 | 车型 | |
| plate_no | VARCHAR | 20 | 否 | 车牌号 | |
| owner_id | BIGINT | 20 | 是 | 车主ID | 关联 sys_user |
| bind_time | DATETIME | | 是 | 绑定时间 | |

### 3.2 `maintenance_record` (维修记录表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| record_id | BIGINT | 20 | 是 | 记录ID | 主键, 自增 |
| battery_id | VARCHAR | 64 | 是 | 电池ID | 关联 battery_info |
| station_id | BIGINT | 20 | 是 | 维修站ID | 关联 sys_dept |
| fault_type | VARCHAR | 50 | 是 | 故障类型 | 字典: fault_type |
| description | VARCHAR | 500 | 否 | 故障描述 | |
| solution | VARCHAR | 500 | 否 | 处理措施 | |
| replace_parts | VARCHAR | 200 | 否 | 更换配件 | |
| maintainer | VARCHAR | 50 | 是 | 维修人员 | |
| create_time | DATETIME | | 是 | 维修时间 | |
| tx_hash | VARCHAR | 66 | 否 | 交易哈希 | 上链凭证 |

---

## 4. 回收与利用模块 (Recycling)

### 4.1 `recycling_appraisal` (回收评估表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| appraisal_id | BIGINT | 20 | 是 | 评估ID | 主键, 自增 |
| battery_id | VARCHAR | 64 | 是 | 电池ID | 关联 battery_info |
| recycler_id | BIGINT | 20 | 是 | 回收商ID | 关联 sys_dept |
| appearance | VARCHAR | 200 | 否 | 外观检查 | |
| residual_value | DECIMAL | 10,2 | 否 | 估算残值 | 单位: 元 |
| suggestion | VARCHAR | 20 | 是 | 处置建议 | 1:梯次利用, 2:拆解再生 |
| appraiser | VARCHAR | 50 | 是 | 评估人 | |
| create_time | DATETIME | | 是 | 评估时间 | |
| data_hash | VARCHAR | 66 | 是 | 数据哈希 | 上链凭证 |

---

## 5. 溯源与审计模块 (Traceability & Audit)

### 5.1 `battery_transfer_record` (流转记录表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| id | BIGINT | 20 | 是 | ID | 主键, 自增 |
| battery_id | VARCHAR | 64 | 是 | 电池ID | 关联 battery_info |
| from_owner | BIGINT | 20 | 是 | 转出方ID | 关联 sys_user |
| to_owner | BIGINT | 20 | 是 | 接收方ID | 关联 sys_user |
| action_type | VARCHAR | 20 | 是 | 动作类型 | PRODUCE, SELL, RECYCLE |
| create_time | DATETIME | | 是 | 发生时间 | |
| tx_hash | VARCHAR | 66 | 否 | 交易哈希 | 关联 chain_transaction |

### 5.2 `chain_transaction` (上链日志表)
| 字段名 | 类型 | 长度 | 必填 | 描述 | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| tx_hash | VARCHAR | 66 | 是 | 交易哈希 | 主键 |
| block_height | BIGINT | 20 | 是 | 区块高度 | |
| contract_addr | VARCHAR | 42 | 是 | 合约地址 | |
| method_name | VARCHAR | 50 | 是 | 调用方法 | |
| params | TEXT | | 否 | 调用参数 | JSON格式存储 |
| status | INT | 1 | 是 | 交易状态 | 0:成功, 1:失败 |
| create_time | DATETIME | | 是 | 上链时间 | |
