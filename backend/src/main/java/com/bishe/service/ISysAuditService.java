package com.bishe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bishe.entity.SysAudit;

public interface ISysAuditService extends IService<SysAudit> {
    /**
     * 提交审核申请
     */
    void submitAudit(String businessType, String businessId, String applyUser);

    /**
     * 执行审核
     */
    void doAudit(Long auditId, Integer status, String auditOpinion, String auditor);
}
