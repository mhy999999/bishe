package com.bishe.service.impl;

import com.bishe.entity.BatteryAlarm;
import com.bishe.entity.HealthMonitor;
import com.bishe.mapper.HealthMonitorMapper;
import com.bishe.service.IBatteryAlarmService;
import com.bishe.service.IHealthMonitorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class HealthMonitorServiceImpl extends ServiceImpl<HealthMonitorMapper, HealthMonitor> implements IHealthMonitorService {

    @Autowired
    private IBatteryAlarmService alarmService;

    @Override
    public boolean save(HealthMonitor entity) {
        if (entity.getReportTime() == null) {
            entity.setReportTime(LocalDateTime.now());
        }

        // 异常检测 logic
        // 温度过高报警 (> 60度)
        if (entity.getMaxTemp() != null && entity.getMaxTemp().compareTo(new BigDecimal("60")) > 0) {
            BatteryAlarm alarm = new BatteryAlarm();
            alarm.setBatteryId(entity.getBatteryId());
            alarm.setAlarmLevel("High");
            alarm.setAlarmContent("电池温度过高: " + entity.getMaxTemp() + "°C");
            alarm.setStatus(0); // 0: 未处理
            alarm.setCreateTime(LocalDateTime.now());
            alarmService.save(alarm);
        }

        // 电量过低报警 (< 20%)
        if (entity.getSoc() != null && entity.getSoc().compareTo(new BigDecimal("20")) < 0) {
            BatteryAlarm alarm = new BatteryAlarm();
            alarm.setBatteryId(entity.getBatteryId());
            alarm.setAlarmLevel("Low");
            alarm.setAlarmContent("电池电量过低: " + entity.getSoc() + "%");
            alarm.setStatus(0);
            alarm.setCreateTime(LocalDateTime.now());
            alarmService.save(alarm);
        }

        return super.save(entity);
    }
}
