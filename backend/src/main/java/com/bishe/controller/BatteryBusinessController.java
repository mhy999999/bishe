package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.*;
import com.bishe.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

/**
 * 电池业务统一控制器
 * 包含: 电池信息, 生产批次, 质检记录, 健康监测, 故障报警, 维修保养, 销售管理
 */
@RestController
@RequestMapping("/battery")
public class BatteryBusinessController {

    @Autowired
    private IBatteryInfoService batteryInfoService;
    @Autowired
    private IProductionBatchService productionBatchService;
    @Autowired
    private IQualityInspectionService qualityInspectionService;
    @Autowired
    private IHealthMonitorService healthMonitorService;
    @Autowired
    private IBatteryAlarmService batteryAlarmService;
    @Autowired
    private IMaintenanceRecordService maintenanceRecordService;
    @Autowired
    private ISalesRecordService salesRecordService;
    @Autowired
    private ISysAuditService sysAuditService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysRoleService sysRoleService;

    private Long getCurrentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long userId) {
            return userId;
        }
        if (userIdObj instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    private boolean hasAnyRole(Long userId, String... roleKeys) {
        if (userId == null || roleKeys == null || roleKeys.length == 0) {
            return false;
        }
        java.util.Set<String> roles = sysRoleService.getRoleKeysByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String roleKey : roleKeys) {
            if (StringUtils.hasText(roleKey) && roles.contains(roleKey)) {
                return true;
            }
        }
        return false;
    }

    private String getCurrentUserDisplayName(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private <T> Result<T> forbidden() {
        return Result.error(403, "无权限访问");
    }

    // ==================== 电池信息 (BatteryInfo) ====================

    @GetMapping("/info/list")
    public Result<Page<BatteryInfo>> listInfo(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String batteryId,
                                              @RequestParam(required = false) Long batchId,
                                              @RequestParam(required = false) Integer status) {
        Page<BatteryInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BatteryInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), BatteryInfo::getBatteryId, batteryId);
        wrapper.eq(batchId != null, BatteryInfo::getBatchId, batchId);
        wrapper.eq(status != null, BatteryInfo::getStatus, status);
        wrapper.orderByDesc(BatteryInfo::getCreateTime);
        return Result.success(batteryInfoService.page(page, wrapper));
    }

    @PostMapping("/sales/audit")
    public Result<Boolean> auditSales(@RequestBody SalesRecord auditData, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        if (auditData == null || auditData.getSalesId() == null) {
            return Result.error(400, "销售ID不能为空");
        }
        if (auditData.getStatus() == null || (auditData.getStatus() != 1 && auditData.getStatus() != 2)) {
            return Result.error(400, "审核状态不合法");
        }
        String auditor = getCurrentUserDisplayName(request);
        if (!StringUtils.hasText(auditor)) {
            auditor = "system";
        }
        LambdaQueryWrapper<SysAudit> query = new LambdaQueryWrapper<>();
        query.eq(SysAudit::getBusinessType, "SALES");
        query.eq(SysAudit::getBusinessId, auditData.getSalesId().toString());
        query.eq(SysAudit::getStatus, 0);
        query.orderByDesc(SysAudit::getApplyTime);
        query.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(query);

        if (audit != null) {
            SalesRecord record = salesRecordService.getById(auditData.getSalesId());
            if (record == null) {
                return Result.error("记录不存在");
            }
            if (!StringUtils.hasText(record.getBatteryId())) {
                return Result.error(400, "电池ID不能为空");
            }
            BatteryInfo battery = batteryInfoService.getById(record.getBatteryId());
            if (battery == null) {
                return Result.error(400, "电池不存在");
            }
            if (auditData.getStatus() == 1) {
                if (battery.getStatus() == null || battery.getStatus() != 1) {
                    return Result.error(400, "仅允许审核通过“已上架”的电池销售");
                }
            }
            sysAuditService.doAudit(audit.getAuditId(), auditData.getStatus(), auditData.getAuditOpinion(), auditor);
        } else {
            SalesRecord record = salesRecordService.getById(auditData.getSalesId());
            if (record == null) return Result.error("记录不存在");

            record.setStatus(auditData.getStatus());
            record.setAuditOpinion(auditData.getAuditOpinion());
            salesRecordService.updateById(record);

            if (auditData.getStatus() == 1 && record.getBatteryId() != null) {
                BatteryInfo existingBattery = batteryInfoService.getById(record.getBatteryId());
                if (existingBattery == null) {
                    return Result.error(400, "电池不存在");
                }
                if (existingBattery.getStatus() != null && existingBattery.getStatus() == 4) {
                    return Result.error(400, "电池已销售");
                }
                BatteryInfo batteryInfo = new BatteryInfo();
                batteryInfo.setBatteryId(record.getBatteryId());
                batteryInfo.setStatus(4);
                batteryInfoService.updateById(batteryInfo);
            }
        }
        return Result.success(true);
    }

    @GetMapping("/info/{id}")
    public Result<BatteryInfo> getInfoById(@PathVariable String id) {
        return Result.success(batteryInfoService.getById(id));
    }

    @PostMapping("/info")
    public Result<Boolean> saveInfo(@RequestBody BatteryInfo batteryInfo) {
        if (batteryInfo.getCreateTime() == null) {
            batteryInfo.setCreateTime(java.time.LocalDateTime.now());
        }
        if (batteryInfo.getBatchId() != null) {
            String message = validateBatchCapacity(batteryInfo.getBatchId(), null);
            if (message != null) {
                return Result.error(400, message);
            }
        }
        // 如果指定了生产批次，状态为"生产中"(0)，否则为"待生产"(6)
        if (batteryInfo.getBatchId() != null) {
            batteryInfo.setStatus(0);
        } else {
            batteryInfo.setStatus(6);
        }
        return Result.success(batteryInfoService.save(batteryInfo));
    }

    @PutMapping("/info")
    public Result<Boolean> updateInfo(@RequestBody BatteryInfo batteryInfo) {
        if (batteryInfo.getBatchId() != null && StringUtils.hasText(batteryInfo.getBatteryId())) {
            BatteryInfo existing = batteryInfoService.getById(batteryInfo.getBatteryId());
            if (existing == null) {
                return Result.error(400, "电池不存在");
            }
            if (!Objects.equals(existing.getBatchId(), batteryInfo.getBatchId())) {
                String message = validateBatchCapacity(batteryInfo.getBatchId(), existing.getBatteryId());
                if (message != null) {
                    return Result.error(400, message);
                }
            }
        }
        // 如果更新时有了批次ID，且之前的状态可能是待生产，则更新为生产中
        // 为了简化逻辑，只要前端传了batchId，且状态是6(待生产)，就改为0
        if (batteryInfo.getBatchId() != null && (batteryInfo.getStatus() == null || batteryInfo.getStatus() == 6)) {
            batteryInfo.setStatus(0);
        }
        return Result.success(batteryInfoService.updateById(batteryInfo));
    }

    @DeleteMapping("/info/{id}")
    public Result<Boolean> removeInfo(@PathVariable String id) {
        return Result.success(batteryInfoService.removeById(id));
    }

    // ==================== 生产批次 (ProductionBatch) ====================

    @GetMapping("/batch/list")
    public Result<Page<ProductionBatch>> listBatch(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ProductionBatch> page = new Page<>(pageNum, pageSize);
        return Result.success(productionBatchService.page(page));
    }

    @PostMapping("/batch")
    public Result<Boolean> saveBatch(@RequestBody ProductionBatch batch) {
        if (batch.getQuantity() == null || batch.getQuantity() <= 0) {
            return Result.error(400, "最大生产数量必须大于0");
        }
        return Result.success(productionBatchService.save(batch));
    }

    /**
     * 结束生产批次，将该批次下所有电池状态改为"待质检"(3)
     */
    @PostMapping("/batch/end/{batchId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> endProductionBatch(@PathVariable Long batchId) {
        // 1. 查找该批次下的所有电池
        LambdaQueryWrapper<BatteryInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BatteryInfo::getBatchId, batchId);
        queryWrapper.eq(BatteryInfo::getStatus, 0); // 仅处理"生产中"的电池
        java.util.List<BatteryInfo> batteries = batteryInfoService.list(queryWrapper);

        if (batteries == null || batteries.isEmpty()) {
            return Result.error("该批次下没有生产中的电池");
        }

        // 2. 批量更新电池状态为 3 (待质检)
        for (BatteryInfo battery : batteries) {
            battery.setStatus(3); // 3: 待质检
        }
        
        return Result.success(batteryInfoService.updateBatchById(batteries));
    }

    private String validateBatchCapacity(Long batchId, String excludeBatteryId) {
        ProductionBatch batch = productionBatchService.getById(batchId);
        if (batch == null) {
            return "生产批次不存在";
        }
        Integer maxQuantity = batch.getQuantity();
        if (maxQuantity == null || maxQuantity <= 0) {
            return "该批次未设置最大生产数量，无法添加电池";
        }
        LambdaQueryWrapper<BatteryInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatteryInfo::getBatchId, batchId);
        wrapper.eq(BatteryInfo::getStatus, 0);
        if (StringUtils.hasText(excludeBatteryId)) {
            wrapper.ne(BatteryInfo::getBatteryId, excludeBatteryId);
        }
        long current = batteryInfoService.count(wrapper);
        if (current >= maxQuantity) {
            return "该批次已达到最大生产数量（上限" + maxQuantity + "），当前生产中" + current + "个，无法继续添加";
        }
        return null;
    }

    // ==================== 质检记录 (QualityInspection) ====================

    @GetMapping("/quality/list")
    public Result<Page<QualityInspection>> listQuality(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String batteryId,
                                                       @RequestParam(required = false) Integer status) {
        Page<QualityInspection> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), QualityInspection::getBatteryId, batteryId);
        wrapper.eq(status != null, QualityInspection::getStatus, status);
        wrapper.orderByDesc(QualityInspection::getCheckTime);
        return Result.success(qualityInspectionService.page(page, wrapper));
    }

    @PostMapping("/quality")
    public Result<Boolean> saveQuality(@RequestBody QualityInspection inspection, HttpServletRequest request) {
        inspection.setStatus(0); // 默认待审核
        if (inspection.getCheckTime() == null) {
            inspection.setCheckTime(java.time.LocalDateTime.now());
        }
        if (!StringUtils.hasText(inspection.getInspector())) {
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj instanceof Long userId) {
                SysUser user = sysUserService.getById(userId);
                if (user != null) {
                    inspection.setInspector(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
                }
            }
        }
        boolean success = qualityInspectionService.save(inspection);
        if (success) {
            String submitter = StringUtils.hasText(inspection.getInspector()) ? inspection.getInspector() : "system";
            sysAuditService.submitAudit("QUALITY", inspection.getId().toString(), submitter);
        }
        return Result.success(success);
    }

    @PutMapping("/quality")
    public Result<Boolean> updateQuality(@RequestBody QualityInspection inspection) {
        inspection.setStatus(0); // 修改后重置为待审核
        if (inspection.getCheckTime() == null) {
            inspection.setCheckTime(java.time.LocalDateTime.now());
        }
        boolean success = qualityInspectionService.updateById(inspection);
        if (success) {
            sysAuditService.submitAudit("QUALITY", inspection.getId().toString(), inspection.getInspector());
        }
        return Result.success(success);
    }

    @PostMapping("/quality/audit")
    public Result<Boolean> auditQuality(@RequestBody QualityInspection auditData) {
        // 查找关联的待审核任务
        LambdaQueryWrapper<SysAudit> query = new LambdaQueryWrapper<>();
        query.eq(SysAudit::getBusinessType, "QUALITY");
        query.eq(SysAudit::getBusinessId, auditData.getId().toString());
        query.eq(SysAudit::getStatus, 0); // 待审核
        query.orderByDesc(SysAudit::getApplyTime);
        query.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(query);

        if (audit != null) {
            // 如果存在审核任务，走统一审核流程（会自动更新业务状态和电池状态）
            sysAuditService.doAudit(audit.getAuditId(), auditData.getStatus(), auditData.getAuditOpinion(), "admin");
        } else {
            // 如果不存在审核任务（异常情况），手动更新业务数据
            QualityInspection inspection = qualityInspectionService.getById(auditData.getId());
            if (inspection == null) {
                return Result.error("记录不存在");
            }
            inspection.setStatus(auditData.getStatus());
            inspection.setAuditOpinion(auditData.getAuditOpinion());
            inspection.setAuditor("admin");
            inspection.setAuditTime(java.time.LocalDateTime.now());
            qualityInspectionService.updateById(inspection);

            // 手动更新电池状态
            if (inspection.getBatteryId() != null) {
                BatteryInfo batteryInfo = new BatteryInfo();
                batteryInfo.setBatteryId(inspection.getBatteryId());
                if (auditData.getStatus() == 1) { // 审核通过
                    batteryInfo.setStatus(1); // 1: 上架 (质检通过)
                } else if (auditData.getStatus() == 2) { // 审核驳回
                    batteryInfo.setStatus(2); // 2: 已废弃
                }
                if (batteryInfo.getStatus() != null) {
                    batteryInfoService.updateById(batteryInfo);
                }
            }
        }
        return Result.success(true);
    }

    // ==================== 健康监测 (HealthMonitor) ====================

    @GetMapping("/health/list")
    public Result<Page<HealthMonitor>> listHealth(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) String batteryId) {
        Page<HealthMonitor> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HealthMonitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), HealthMonitor::getBatteryId, batteryId);
        wrapper.orderByDesc(HealthMonitor::getReportTime);
        return Result.success(healthMonitorService.page(page, wrapper));
    }

    @PostMapping("/health")
    public Result<Boolean> saveHealth(@RequestBody HealthMonitor monitor) {
        return Result.success(healthMonitorService.save(monitor));
    }

    // ==================== 故障报警 (BatteryAlarm) ====================

    @GetMapping("/alarm/list")
    public Result<Page<BatteryAlarm>> listAlarm(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String batteryId) {
        Page<BatteryAlarm> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BatteryAlarm> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), BatteryAlarm::getBatteryId, batteryId);
        wrapper.orderByDesc(BatteryAlarm::getCreateTime);
        return Result.success(batteryAlarmService.page(page, wrapper));
    }

    @PostMapping("/alarm")
    public Result<Boolean> saveAlarm(@RequestBody BatteryAlarm alarm) {
        return Result.success(batteryAlarmService.save(alarm));
    }

    @PutMapping("/alarm")
    public Result<Boolean> updateAlarm(@RequestBody BatteryAlarm alarm) {
        return Result.success(batteryAlarmService.updateById(alarm));
    }

    // ==================== 维修保养 (Maintenance) ====================

    @GetMapping("/maintenance/list")
    public Result<Page<MaintenanceRecord>> listMaintenance(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) Integer status,
                                                         HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "maintainer", "maintenance")) {
            return forbidden();
        }
        Page<MaintenanceRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, MaintenanceRecord::getStatus, status);
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);
        return Result.success(maintenanceRecordService.page(page, wrapper));
    }

    @PostMapping("/maintenance/audit")
    public Result<Boolean> auditMaintenance(@RequestBody MaintenanceRecord auditData, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (auditData == null || auditData.getRecordId() == null) {
            return Result.error(400, "记录ID不能为空");
        }
        if (auditData.getStatus() == null || (auditData.getStatus() != 1 && auditData.getStatus() != 2)) {
            return Result.error(400, "审核状态不合法");
        }
        String auditor = getCurrentUserDisplayName(request);
        if (!StringUtils.hasText(auditor)) {
            auditor = "system";
        }
        // 查找关联的待审核任务
        LambdaQueryWrapper<SysAudit> query = new LambdaQueryWrapper<>();
        query.eq(SysAudit::getBusinessType, "MAINTENANCE");
        query.eq(SysAudit::getBusinessId, auditData.getRecordId().toString());
        query.eq(SysAudit::getStatus, 0); // 待审核
        query.orderByDesc(SysAudit::getApplyTime);
        query.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(query);

        if (audit != null) {
            sysAuditService.doAudit(audit.getAuditId(), auditData.getStatus(), auditData.getAuditOpinion(), auditor);
        } else {
            MaintenanceRecord record = maintenanceRecordService.getById(auditData.getRecordId());
            if (record == null) return Result.error("记录不存在");
            record.setStatus(auditData.getStatus());
            record.setAuditOpinion(auditData.getAuditOpinion());
            maintenanceRecordService.updateById(record);
        }
        return Result.success(true);
    }

    @PostMapping("/maintenance")
    public Result<Boolean> saveMaintenance(@RequestBody MaintenanceRecord record, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (record == null) {
            return Result.error(400, "参数不能为空");
        }
        if (!StringUtils.hasText(record.getBatteryId())) {
            return Result.error(400, "电池ID不能为空");
        }
        if (!StringUtils.hasText(record.getFaultType())) {
            return Result.error(400, "故障类型不能为空");
        }
        if (!StringUtils.hasText(record.getDescription())) {
            return Result.error(400, "维修内容不能为空");
        }
        BatteryInfo battery = batteryInfoService.getById(record.getBatteryId());
        if (battery == null) {
            return Result.error(400, "电池不存在");
        }
        if (battery.getStatus() != null && (battery.getStatus() == 2 || battery.getStatus() == 6)) {
            return Result.error(400, "当前电池状态不允许提交维修");
        }
        if (!StringUtils.hasText(record.getMaintainer())) {
            record.setMaintainer(getCurrentUserDisplayName(request));
        }
        if (!StringUtils.hasText(record.getMaintainer())) {
            return Result.error(400, "维修人不能为空");
        }
        record.setStatus(0);
        if (record.getCreateTime() == null) record.setCreateTime(java.time.LocalDateTime.now());
        boolean success = maintenanceRecordService.save(record);
        if (success) {
            sysAuditService.submitAudit("MAINTENANCE", record.getRecordId().toString(), record.getMaintainer());
        }
        return Result.success(success);
    }

    @PutMapping("/maintenance")
    public Result<Boolean> updateMaintenance(@RequestBody MaintenanceRecord record, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (record == null || record.getRecordId() == null) {
            return Result.error(400, "记录ID不能为空");
        }
        MaintenanceRecord existing = maintenanceRecordService.getById(record.getRecordId());
        if (existing == null) {
            return Result.error("记录不存在");
        }
        if (existing.getStatus() != null && existing.getStatus() == 1 && !hasAnyRole(userId, "admin")) {
            return Result.error(403, "已通过记录不允许修改");
        }
        String batteryIdToUse = StringUtils.hasText(record.getBatteryId()) ? record.getBatteryId() : existing.getBatteryId();
        if (!StringUtils.hasText(batteryIdToUse)) {
            return Result.error(400, "电池ID不能为空");
        }
        BatteryInfo battery = batteryInfoService.getById(batteryIdToUse);
        if (battery == null) {
            return Result.error(400, "电池不存在");
        }
        if (battery.getStatus() != null && (battery.getStatus() == 2 || battery.getStatus() == 6)) {
            return Result.error(400, "当前电池状态不允许提交维修");
        }
        String maintainerToUse = StringUtils.hasText(record.getMaintainer()) ? record.getMaintainer() : existing.getMaintainer();
        if (!StringUtils.hasText(maintainerToUse)) {
            maintainerToUse = getCurrentUserDisplayName(request);
        }
        if (!StringUtils.hasText(maintainerToUse)) {
            return Result.error(400, "维修人不能为空");
        }

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MaintenanceRecord> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(MaintenanceRecord::getRecordId, record.getRecordId());
        updateWrapper.set(MaintenanceRecord::getStatus, 0);
        updateWrapper.set(StringUtils.hasText(record.getBatteryId()), MaintenanceRecord::getBatteryId, record.getBatteryId());
        updateWrapper.set(record.getStationId() != null, MaintenanceRecord::getStationId, record.getStationId());
        updateWrapper.set(StringUtils.hasText(record.getFaultType()), MaintenanceRecord::getFaultType, record.getFaultType());
        updateWrapper.set(record.getDescription() != null, MaintenanceRecord::getDescription, record.getDescription());
        updateWrapper.set(record.getSolution() != null, MaintenanceRecord::getSolution, record.getSolution());
        updateWrapper.set(record.getReplaceParts() != null, MaintenanceRecord::getReplaceParts, record.getReplaceParts());
        updateWrapper.set(StringUtils.hasText(record.getMaintainer()), MaintenanceRecord::getMaintainer, record.getMaintainer());
        updateWrapper.set(!StringUtils.hasText(existing.getMaintainer()), MaintenanceRecord::getMaintainer, maintainerToUse);
        boolean success = maintenanceRecordService.update(updateWrapper);
        if (success) {
            sysAuditService.submitAudit("MAINTENANCE", record.getRecordId().toString(), maintainerToUse);
        }
        return Result.success(success);
    }

    // ==================== 销售管理 (Sales) ====================

    @GetMapping("/sales/list")
    public Result<Page<SalesRecord>> listSales(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Integer status,
                                               HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        Page<SalesRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SalesRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, SalesRecord::getStatus, status);
        wrapper.orderByDesc(SalesRecord::getSalesDate);
        return Result.success(salesRecordService.page(page, wrapper));
    }

    @PostMapping("/sales")
    public Result<Boolean> saveSales(@RequestBody SalesRecord record, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        if (record == null) {
            return Result.error(400, "参数不能为空");
        }
        if (!StringUtils.hasText(record.getBatteryId())) {
            return Result.error(400, "电池ID不能为空");
        }
        if (!StringUtils.hasText(record.getBuyerName())) {
            return Result.error(400, "买家姓名不能为空");
        }
        BigDecimal price = record.getSalesPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(400, "售价必须大于0");
        }
        if (!StringUtils.hasText(record.getMaterialDesc())) {
            return Result.error(400, "材料说明不能为空");
        }
        if (!StringUtils.hasText(record.getMaterialUrl())) {
            record.setMaterialUrl("[]");
        }
        BatteryInfo battery = batteryInfoService.getById(record.getBatteryId());
        if (battery == null) {
            return Result.error(400, "电池不存在");
        }
        if (battery.getStatus() == null || battery.getStatus() != 1) {
            return Result.error(400, "仅允许销售状态为“已上架”的电池");
        }
        LambdaQueryWrapper<SalesRecord> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(SalesRecord::getBatteryId, record.getBatteryId());
        pendingWrapper.eq(SalesRecord::getStatus, 0);
        if (salesRecordService.count(pendingWrapper) > 0) {
            return Result.error(400, "该电池已有待审核销售记录");
        }
        if (!StringUtils.hasText(record.getSalesPerson())) {
            record.setSalesPerson(getCurrentUserDisplayName(request));
        }
        if (!StringUtils.hasText(record.getSalesPerson())) {
            return Result.error(400, "销售员不能为空");
        }
        record.setStatus(0);
        if (record.getSalesDate() == null) record.setSalesDate(java.time.LocalDateTime.now());
        boolean success = salesRecordService.save(record);
        if (success) {
            sysAuditService.submitAudit("SALES", record.getSalesId().toString(), record.getSalesPerson());
        }
        return Result.success(success);
    }

    @PutMapping("/sales")
    public Result<Boolean> updateSales(@RequestBody SalesRecord record, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        if (record == null || record.getSalesId() == null) {
            return Result.error(400, "销售ID不能为空");
        }
        SalesRecord existing = salesRecordService.getById(record.getSalesId());
        if (existing == null) {
            return Result.error("记录不存在");
        }
        if (existing.getStatus() != null && existing.getStatus() == 1 && !hasAnyRole(userId, "admin")) {
            return Result.error(403, "已通过记录不允许修改");
        }
        if (existing.getStatus() != null && existing.getStatus() == 3) {
            return Result.error(403, "已取消记录不允许修改");
        }
        String batteryIdToUse = StringUtils.hasText(record.getBatteryId()) ? record.getBatteryId() : existing.getBatteryId();
        if (!StringUtils.hasText(batteryIdToUse)) {
            return Result.error(400, "电池ID不能为空");
        }
        BatteryInfo battery = batteryInfoService.getById(batteryIdToUse);
        if (battery == null) {
            return Result.error(400, "电池不存在");
        }
        if (battery.getStatus() == null || battery.getStatus() != 1) {
            return Result.error(400, "仅允许销售状态为“已上架”的电池");
        }
        if (StringUtils.hasText(record.getBatteryId()) && !Objects.equals(existing.getBatteryId(), record.getBatteryId())) {
            LambdaQueryWrapper<SalesRecord> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.eq(SalesRecord::getBatteryId, record.getBatteryId());
            pendingWrapper.eq(SalesRecord::getStatus, 0);
            pendingWrapper.ne(SalesRecord::getSalesId, record.getSalesId());
            if (salesRecordService.count(pendingWrapper) > 0) {
                return Result.error(400, "该电池已有待审核销售记录");
            }
        }
        String buyerNameToUse = StringUtils.hasText(record.getBuyerName()) ? record.getBuyerName() : existing.getBuyerName();
        if (!StringUtils.hasText(buyerNameToUse)) {
            return Result.error(400, "买家姓名不能为空");
        }
        BigDecimal price = record.getSalesPrice() != null ? record.getSalesPrice() : existing.getSalesPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(400, "售价必须大于0");
        }
        String materialDescToUse = StringUtils.hasText(record.getMaterialDesc()) ? record.getMaterialDesc() : existing.getMaterialDesc();
        if (!StringUtils.hasText(materialDescToUse)) {
            return Result.error(400, "材料说明不能为空");
        }
        String materialUrlToUse = record.getMaterialUrl() != null ? record.getMaterialUrl() : existing.getMaterialUrl();
        if (!StringUtils.hasText(materialUrlToUse)) {
            materialUrlToUse = "[]";
        }
        String salesPersonToUse = StringUtils.hasText(record.getSalesPerson()) ? record.getSalesPerson() : existing.getSalesPerson();
        if (!StringUtils.hasText(salesPersonToUse)) {
            salesPersonToUse = getCurrentUserDisplayName(request);
        }
        if (!StringUtils.hasText(salesPersonToUse)) {
            return Result.error(400, "销售员不能为空");
        }

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SalesRecord> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(SalesRecord::getSalesId, record.getSalesId());
        updateWrapper.set(SalesRecord::getStatus, 0);
        updateWrapper.set(SalesRecord::getAuditOpinion, null);
        updateWrapper.set(StringUtils.hasText(record.getBatteryId()), SalesRecord::getBatteryId, record.getBatteryId());
        updateWrapper.set(StringUtils.hasText(record.getBuyerName()), SalesRecord::getBuyerName, record.getBuyerName());
        updateWrapper.set(record.getSalesPrice() != null, SalesRecord::getSalesPrice, record.getSalesPrice());
        updateWrapper.set(record.getSalesDate() != null, SalesRecord::getSalesDate, record.getSalesDate());
        updateWrapper.set(StringUtils.hasText(record.getSalesPerson()), SalesRecord::getSalesPerson, record.getSalesPerson());
        updateWrapper.set(!StringUtils.hasText(existing.getSalesPerson()), SalesRecord::getSalesPerson, salesPersonToUse);
        updateWrapper.set(SalesRecord::getMaterialDesc, materialDescToUse);
        updateWrapper.set(SalesRecord::getMaterialUrl, materialUrlToUse);
        boolean success = salesRecordService.update(updateWrapper);
        if (success) {
            sysAuditService.submitAudit("SALES", record.getSalesId().toString(), salesPersonToUse);
        }
        return Result.success(success);
    }

    @PostMapping("/sales/cancel/{salesId}")
    public Result<Boolean> cancelSales(@PathVariable("salesId") Long salesId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        if (salesId == null) {
            return Result.error(400, "销售ID不能为空");
        }
        SalesRecord existing = salesRecordService.getById(salesId);
        if (existing == null) {
            return Result.error("记录不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 2) {
            return Result.error(400, "仅允许取消已驳回的销售记录");
        }

        LambdaQueryWrapper<SysAudit> pendingAuditQuery = new LambdaQueryWrapper<>();
        pendingAuditQuery.eq(SysAudit::getBusinessType, "SALES");
        pendingAuditQuery.eq(SysAudit::getBusinessId, salesId.toString());
        pendingAuditQuery.eq(SysAudit::getStatus, 0);
        if (sysAuditService.count(pendingAuditQuery) > 0) {
            return Result.error(400, "记录处于待审核状态，无法取消");
        }

        SalesRecord update = new SalesRecord();
        update.setSalesId(salesId);
        update.setStatus(3);
        return Result.success(salesRecordService.updateById(update));
    }

    @PostMapping("/sales/material/upload")
    public Result<String> uploadSalesMaterial(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过10MB");
        }
        String originalFilename = file.getOriginalFilename();
        String safeName = (originalFilename == null ? "file" : originalFilename)
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_");
        String savedName = UUID.randomUUID().toString().replace("-", "") + "_" + safeName;
        Path targetDir = Paths.get(System.getProperty("user.dir"), "uploads", "sales");
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(savedName);
            file.transferTo(targetFile.toFile());
            String url = "/files/sales/" + savedName;
            return Result.success(url);
        } catch (Exception e) {
            return Result.error(500, "上传失败");
        }
    }

    @GetMapping("/sales/material/preview/doc")
    public Result<String> previewSalesMaterialDoc(@RequestParam("url") String url, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "sales", "dealer")) {
            return forbidden();
        }
        if (!StringUtils.hasText(url)) {
            return Result.error(400, "文件地址不能为空");
        }
        String text = url.trim();
        int qIndex = text.indexOf('?');
        if (qIndex >= 0) {
            text = text.substring(0, qIndex);
        }
        int marker = text.indexOf("/files/sales/");
        String fileName = marker >= 0 ? text.substring(marker + "/files/sales/".length()) : text;
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        if (!StringUtils.hasText(fileName)) {
            return Result.error(400, "文件名不能为空");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return Result.error(400, "非法文件路径");
        }
        if (!fileName.toLowerCase().endsWith(".doc")) {
            return Result.error(400, "仅支持doc格式预览");
        }

        Path targetDir = Paths.get(System.getProperty("user.dir"), "uploads", "sales").normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();
        if (!targetFile.startsWith(targetDir)) {
            return Result.error(400, "非法文件路径");
        }
        if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            return Result.error(404, "文件不存在");
        }
        try (InputStream in = Files.newInputStream(targetFile);
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            String content = extractor.getText();
            if (content == null) {
                content = "";
            }
            content = content.replace("\u0000", "");
            return Result.success(content);
        } catch (Exception e) {
            return Result.error(500, "预览解析失败");
        }
    }
}
