package com.bishe.service.impl;

import com.bishe.entity.BatteryInfo;
import com.bishe.mapper.BatteryInfoMapper;
import com.bishe.service.IBatteryInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BatteryInfoServiceImpl extends ServiceImpl<BatteryInfoMapper, BatteryInfo> implements IBatteryInfoService {
}
