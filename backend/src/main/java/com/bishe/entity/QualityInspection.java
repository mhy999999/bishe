package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("quality_inspection")
public class QualityInspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batteryId;
    private BigDecimal ocv;
    private BigDecimal acr;
    private BigDecimal insulationRes;
    private String airTightness;
    private String inspector;
    private LocalDateTime checkTime;
    private String dataHash;
    /**
     * 状态: 0-待审核, 1-已通过, 2-已驳回
     */
    private Integer status;
    private String auditOpinion;
    private String auditor;
    private LocalDateTime auditTime;
}
