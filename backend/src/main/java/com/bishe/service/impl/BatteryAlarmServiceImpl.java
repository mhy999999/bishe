package com.bishe.service.impl;

import com.bishe.entity.BatteryAlarm;
import com.bishe.mapper.BatteryAlarmMapper;
import com.bishe.service.IBatteryAlarmService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BatteryAlarmServiceImpl extends ServiceImpl<BatteryAlarmMapper, BatteryAlarm> implements IBatteryAlarmService {
}
