package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recycling_appraisal")
public class RecyclingAppraisal {
    @TableId(type = IdType.AUTO)
    private Long appraisalId;
    private String batteryId;
    private Long recyclerId;
    private String appearance;
    private BigDecimal residualValue;
    private String suggestion;
    private String appraiser;
    private LocalDateTime createTime;
    private String dataHash;

    private Integer status;
    private String applyReason;
    private String applyUser;
    private LocalDateTime applyTime;
    private BigDecimal snapshotVoltage;
    private BigDecimal snapshotCapacity;
    private Integer snapshotBatteryStatus;
    private String recycleNo;
    private String photoUrls;
    private String performanceReportUrl;
    private String maintenanceHistoryJson;
    private String valuationBasis;
    private BigDecimal preliminaryValue;
    private BigDecimal finalValue;
    private String priceReviewer;
    private LocalDateTime recycleTime;
    private String receiptHash;
    private LocalDateTime updateTime;
}
