package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.SysAudit;
import com.bishe.service.ISysAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditController {

    @Autowired
    private ISysAuditService sysAuditService;

    @GetMapping("/list")
    public Result<Page<SysAudit>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String businessType,
                                       @RequestParam(required = false) Integer status) {
        Page<SysAudit> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(businessType), SysAudit::getBusinessType, businessType);
        wrapper.eq(status != null, SysAudit::getStatus, status);
        wrapper.orderByDesc(SysAudit::getApplyTime);
        return Result.success(sysAuditService.page(page, wrapper));
    }

    @PostMapping("/process")
    public Result<Boolean> processAudit(@RequestBody SysAudit auditData) {
        // 实际项目中 auditor 从 SecurityContextHolder 获取
        sysAuditService.doAudit(auditData.getAuditId(), auditData.getStatus(), 
                                auditData.getAuditOpinion(), "admin");
        return Result.success(true);
    }
}
