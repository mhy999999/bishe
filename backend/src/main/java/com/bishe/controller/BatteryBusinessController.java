package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.*;
import com.bishe.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    public Result<Boolean> auditSales(@RequestBody SalesRecord auditData) {
        LambdaQueryWrapper<SysAudit> query = new LambdaQueryWrapper<>();
        query.eq(SysAudit::getBusinessType, "SALES");
        query.eq(SysAudit::getBusinessId, auditData.getSalesId().toString());
        query.eq(SysAudit::getStatus, 0);
        query.orderByDesc(SysAudit::getApplyTime);
        query.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(query);

        if (audit != null) {
            sysAuditService.doAudit(audit.getAuditId(), auditData.getStatus(), auditData.getAuditOpinion(), "admin");
        } else {
            SalesRecord record = salesRecordService.getById(auditData.getSalesId());
            if (record == null) return Result.error("记录不存在");
            
            record.setStatus(auditData.getStatus());
            record.setAuditOpinion(auditData.getAuditOpinion());
            salesRecordService.updateById(record);
            
            if (auditData.getStatus() == 1 && record.getBatteryId() != null) {
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
                                                         @RequestParam(required = false) Integer status) {
        Page<MaintenanceRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, MaintenanceRecord::getStatus, status);
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);
        return Result.success(maintenanceRecordService.page(page, wrapper));
    }

    @PostMapping("/maintenance/audit")
    public Result<Boolean> auditMaintenance(@RequestBody MaintenanceRecord auditData) {
        // 查找关联的待审核任务
        LambdaQueryWrapper<SysAudit> query = new LambdaQueryWrapper<>();
        query.eq(SysAudit::getBusinessType, "MAINTENANCE");
        query.eq(SysAudit::getBusinessId, auditData.getRecordId().toString());
        query.eq(SysAudit::getStatus, 0); // 待审核
        query.orderByDesc(SysAudit::getApplyTime);
        query.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(query);

        if (audit != null) {
            sysAuditService.doAudit(audit.getAuditId(), auditData.getStatus(), auditData.getAuditOpinion(), "admin");
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
    public Result<Boolean> saveMaintenance(@RequestBody MaintenanceRecord record) {
        record.setStatus(0);
        if (record.getCreateTime() == null) record.setCreateTime(java.time.LocalDateTime.now());
        boolean success = maintenanceRecordService.save(record);
        if (success) {
            sysAuditService.submitAudit("MAINTENANCE", record.getRecordId().toString(), record.getMaintainer());
        }
        return Result.success(success);
    }

    @PutMapping("/maintenance")
    public Result<Boolean> updateMaintenance(@RequestBody MaintenanceRecord record) {
        record.setStatus(0);
        boolean success = maintenanceRecordService.updateById(record);
        if (success) {
            sysAuditService.submitAudit("MAINTENANCE", record.getRecordId().toString(), record.getMaintainer());
        }
        return Result.success(success);
    }

    // ==================== 销售管理 (Sales) ====================

    @GetMapping("/sales/list")
    public Result<Page<SalesRecord>> listSales(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Integer status) {
        Page<SalesRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SalesRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, SalesRecord::getStatus, status);
        wrapper.orderByDesc(SalesRecord::getSalesDate);
        return Result.success(salesRecordService.page(page, wrapper));
    }

    @PostMapping("/sales")
    public Result<Boolean> saveSales(@RequestBody SalesRecord record) {
        record.setStatus(0);
        if (record.getSalesDate() == null) record.setSalesDate(java.time.LocalDateTime.now());
        boolean success = salesRecordService.save(record);
        if (success) {
            sysAuditService.submitAudit("SALES", record.getSalesId().toString(), record.getSalesPerson());
        }
        return Result.success(success);
    }

    @PutMapping("/sales")
    public Result<Boolean> updateSales(@RequestBody SalesRecord record) {
        record.setStatus(0);
        boolean success = salesRecordService.updateById(record);
        if (success) {
            sysAuditService.submitAudit("SALES", record.getSalesId().toString(), record.getSalesPerson());
        }
        return Result.success(success);
    }
}
