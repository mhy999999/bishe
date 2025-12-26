package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("battery_alarm")
public class BatteryAlarm {
    @TableId(type = IdType.AUTO)
    private Long alarmId;
    private String batteryId;
    private String alarmLevel;
    private String alarmContent;
    private Integer status;
    private LocalDateTime createTime;
}
