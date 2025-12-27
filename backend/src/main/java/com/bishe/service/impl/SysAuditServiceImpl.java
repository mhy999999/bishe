package com.bishe.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bishe.entity.BatteryInfo;
import com.bishe.entity.MaintenanceRecord;
import com.bishe.entity.QualityInspection;
import com.bishe.entity.SalesRecord;
import com.bishe.entity.SysAudit;
import com.bishe.mapper.SysAuditMapper;
import com.bishe.service.IBatteryInfoService;
import com.bishe.service.IMaintenanceRecordService;
import com.bishe.service.IQualityInspectionService;
import com.bishe.service.ISalesRecordService;
import com.bishe.service.ISysAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SysAuditServiceImpl extends ServiceImpl<SysAuditMapper, SysAudit> implements ISysAuditService {

    @Autowired
    private IQualityInspectionService qualityInspectionService;
    @Autowired
    private IMaintenanceRecordService maintenanceRecordService;
    @Autowired
    private ISalesRecordService salesRecordService;
    @Autowired
    private IBatteryInfoService batteryInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAudit(String businessType, String businessId, String applyUser) {
        SysAudit audit = new SysAudit();
        audit.setBusinessType(businessType);
        audit.setBusinessId(businessId);
        audit.setApplyUser(applyUser);
        audit.setApplyTime(LocalDateTime.now());
        audit.setStatus(0); // 待审核
        this.save(audit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doAudit(Long auditId, Integer status, String auditOpinion, String auditor) {
        SysAudit audit = this.getById(auditId);
        if (audit == null) {
            throw new RuntimeException("审核任务不存在");
        }
        
        // 1. 更新审核表状态
        audit.setStatus(status);
        audit.setAuditOpinion(auditOpinion);
        audit.setAuditor(auditor);
        audit.setAuditTime(LocalDateTime.now());
        this.updateById(audit);

        // 2. 回调更新业务表状态
        String businessType = audit.getBusinessType();
        String businessId = audit.getBusinessId();

        if ("QUALITY".equals(businessType)) {
            QualityInspection inspection = qualityInspectionService.getById(Long.parseLong(businessId));
            
            QualityInspection update = new QualityInspection();
            update.setId(Long.parseLong(businessId));
            update.setStatus(status);
            // 同步审核信息回业务表 (冗余存储，方便查询)
            update.setAuditOpinion(auditOpinion);
            update.setAuditor(auditor);
            update.setAuditTime(audit.getAuditTime());
            qualityInspectionService.updateById(update);

            // 更新电池状态
            if (inspection != null && inspection.getBatteryId() != null) {
                BatteryInfo batteryInfo = new BatteryInfo();
                batteryInfo.setBatteryId(inspection.getBatteryId());
                if (status == 1) { // 审核通过
                    batteryInfo.setStatus(1); // 1: 上架 (质检通过)
                } else if (status == 2) { // 审核驳回
                    batteryInfo.setStatus(2); // 2: 已废弃
                }
                if (batteryInfo.getStatus() != null) {
                    batteryInfoService.updateById(batteryInfo);
                }
            }
        } else if ("MAINTENANCE".equals(businessType)) {
            MaintenanceRecord update = new MaintenanceRecord();
            update.setRecordId(Long.parseLong(businessId));
            update.setStatus(status);
            update.setAuditOpinion(auditOpinion);
            maintenanceRecordService.updateById(update);
        } else if ("SALES".equals(businessType)) {
            SalesRecord sales = salesRecordService.getById(Long.parseLong(businessId));

            SalesRecord update = new SalesRecord();
            update.setSalesId(Long.parseLong(businessId));
            update.setStatus(status);
            update.setAuditOpinion(auditOpinion);
            salesRecordService.updateById(update);

            // 更新电池状态
            if (status == 1 && sales != null && sales.getBatteryId() != null) { // 销售审核通过
                BatteryInfo batteryInfo = new BatteryInfo();
                batteryInfo.setBatteryId(sales.getBatteryId());
                batteryInfo.setStatus(4); // 4: 已销售
                batteryInfoService.updateById(batteryInfo);
            }
        }
    }
}
