package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("battery_info")
public class BatteryInfo {
    @TableId
    private String batteryId;
    private Long batchId;
    private String manufacturer;
    private String typeCode;
    private BigDecimal capacity;
    private BigDecimal voltage;
    private String cathodeMaterial;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate produceDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private Integer status;
}
