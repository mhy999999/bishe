package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales_record")
public class SalesRecord {
    @TableId(type = IdType.AUTO)
    private Long salesId;
    private String batteryId;
    private String buyerName;
    private BigDecimal salesPrice;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime salesDate;
    private String salesPerson;
    private Integer status;
    private String auditOpinion;
}
