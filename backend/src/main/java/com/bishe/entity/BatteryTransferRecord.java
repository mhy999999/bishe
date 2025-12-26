package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("battery_transfer_record")
public class BatteryTransferRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batteryId;
    private Long fromOwner;
    private Long toOwner;
    private String actionType;
    private LocalDateTime createTime;
    private String txHash;
}
