package com.bishe.service.impl;

import cn.hutool.json.JSONUtil;
import com.bishe.entity.VehicleInfo;
import com.bishe.mapper.VehicleInfoMapper;
import com.bishe.service.IChainTransactionService;
import com.bishe.service.IVehicleInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VehicleInfoServiceImpl extends ServiceImpl<VehicleInfoMapper, VehicleInfo> implements IVehicleInfoService {

    @Autowired
    private IChainTransactionService chainService;

    @Override
    public boolean save(VehicleInfo entity) {
        if (entity.getBindTime() == null) {
            entity.setBindTime(LocalDateTime.now());
        }
        // 上链
        String params = JSONUtil.toJsonStr(entity);
        chainService.submitTransaction("bindVehicle", params);
        return super.save(entity);
    }
}
