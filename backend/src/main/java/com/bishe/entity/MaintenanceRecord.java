package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("maintenance_record")
public class MaintenanceRecord {
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private String batteryId;
    private Long stationId;
    private String faultType;
    private String description;
    private String solution;
    private String replaceParts;
    private String maintainer;
    private LocalDateTime createTime;
    private String txHash;
    private Integer status;
    private String auditOpinion;
    private String auditor;
    private LocalDateTime auditTime;
    private String issueMaterialDesc;
    private String issueMaterialUrl;
    private String completionMaterialDesc;
    private String completionMaterialUrl;
    private LocalDateTime completeTime;
}
