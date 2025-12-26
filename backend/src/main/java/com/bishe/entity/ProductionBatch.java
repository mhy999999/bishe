package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("production_batch")
public class ProductionBatch {
    @TableId(type = IdType.AUTO)
    private Long batchId;
    private String batchNo;
    private Long manufacturerId;
    private LocalDate produceDate;
    private Integer quantity;
}
