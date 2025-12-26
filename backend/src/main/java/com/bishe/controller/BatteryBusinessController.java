package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.*;
import com.bishe.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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

    // ==================== 电池信息 (BatteryInfo) ====================

    @GetMapping("/info/list")
    public Result<Page<BatteryInfo>> listInfo(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String batteryId) {
        Page<BatteryInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BatteryInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), BatteryInfo::getBatteryId, batteryId);
        wrapper.orderByDesc(BatteryInfo::getCreateTime);
        return Result.success(batteryInfoService.page(page, wrapper));
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
        return Result.success(batteryInfoService.save(batteryInfo));
    }

    @PutMapping("/info")
    public Result<Boolean> updateInfo(@RequestBody BatteryInfo batteryInfo) {
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

    // ==================== 质检记录 (QualityInspection) ====================

    @GetMapping("/quality/list")
    public Result<Page<QualityInspection>> listQuality(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String batteryId) {
        Page<QualityInspection> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), QualityInspection::getBatteryId, batteryId);
        return Result.success(qualityInspectionService.page(page, wrapper));
    }

    @PostMapping("/quality")
    public Result<Boolean> saveQuality(@RequestBody QualityInspection inspection) {
        inspection.setStatus(0); // 默认待审核
        if (inspection.getCheckTime() == null) {
            inspection.setCheckTime(java.time.LocalDateTime.now());
        }
        boolean success = qualityInspectionService.save(inspection);
        if (success) {
            // 提交到统一审核中心
            sysAuditService.submitAudit("QUALITY", inspection.getId().toString(), inspection.getInspector());
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
        QualityInspection inspection = qualityInspectionService.getById(auditData.getId());
        if (inspection == null) {
            return Result.error("记录不存在");
        }
        inspection.setStatus(auditData.getStatus());
        inspection.setAuditOpinion(auditData.getAuditOpinion());
        // 实际项目中应从 SecurityContext 获取当前用户
        inspection.setAuditor("admin"); 
        inspection.setAuditTime(java.time.LocalDateTime.now());
        return Result.success(qualityInspectionService.updateById(inspection));
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
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<MaintenanceRecord> page = new Page<>(pageNum, pageSize);
        return Result.success(maintenanceRecordService.page(page));
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
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SalesRecord> page = new Page<>(pageNum, pageSize);
        return Result.success(salesRecordService.page(page));
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
