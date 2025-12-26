package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vehicle_info")
public class VehicleInfo {
    @TableId(type = IdType.AUTO)
    private Long vehicleId;
    private String vin;
    private String batteryId;
    private String brand;
    private String model;
    private String plateNo;
    private Long ownerId;
    private LocalDateTime bindTime;
}
