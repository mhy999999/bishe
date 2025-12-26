package com.bishe.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bishe.entity.MaintenanceRecord;
import com.bishe.entity.QualityInspection;
import com.bishe.entity.SalesRecord;
import com.bishe.entity.SysAudit;
import com.bishe.mapper.SysAuditMapper;
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
            QualityInspection update = new QualityInspection();
            update.setId(Long.parseLong(businessId));
            update.setStatus(status);
            // 同步审核信息回业务表 (冗余存储，方便查询)
            update.setAuditOpinion(auditOpinion);
            update.setAuditor(auditor);
            update.setAuditTime(audit.getAuditTime());
            qualityInspectionService.updateById(update);
        } else if ("MAINTENANCE".equals(businessType)) {
            MaintenanceRecord update = new MaintenanceRecord();
            update.setRecordId(Long.parseLong(businessId));
            update.setStatus(status);
            maintenanceRecordService.updateById(update);
        } else if ("SALES".equals(businessType)) {
            SalesRecord update = new SalesRecord();
            update.setSalesId(Long.parseLong(businessId));
            update.setStatus(status);
            salesRecordService.updateById(update);
        }
    }
}
