package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_audit")
public class SysAudit {
    @TableId(type = IdType.AUTO)
    private Long auditId;
    /**
     * 业务类型: QUALITY, MAINTENANCE, SALES
     */
    private String businessType;
    private String businessId;
    private String applyUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    /**
     * 状态: 0-待审核, 1-已通过, 2-已驳回
     */
    private Integer status;
    private String auditor;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    private String auditOpinion;
}
