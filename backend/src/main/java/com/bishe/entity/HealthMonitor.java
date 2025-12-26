package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_monitor")
public class HealthMonitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batteryId;
    private BigDecimal soc;
    private BigDecimal soh;
    private Integer cycleCount;
    private BigDecimal maxTemp;
    private BigDecimal minTemp;
    private LocalDateTime reportTime;
}
