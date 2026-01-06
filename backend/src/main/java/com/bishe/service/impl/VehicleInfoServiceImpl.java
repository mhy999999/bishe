package com.bishe.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.entity.BatteryInfo;
import com.bishe.entity.VehicleInfo;
import com.bishe.mapper.VehicleInfoMapper;
import com.bishe.service.IBatteryInfoService;
import com.bishe.service.IChainTransactionService;
import com.bishe.service.IVehicleInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class VehicleInfoServiceImpl extends ServiceImpl<VehicleInfoMapper, VehicleInfo> implements IVehicleInfoService {

    @Autowired
    private IChainTransactionService chainService;

    @Autowired
    private IBatteryInfoService batteryInfoService;

    private String normalizeVin(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        s = s.replace(" ", "").replace("-", "").toUpperCase();
        return s.length() > 17 ? s.substring(0, 17) : s;
    }

    @Override
    public boolean save(VehicleInfo entity) {
        if (entity == null) {
            throw new RuntimeException("参数不能为空");
        }

        String vin = normalizeVin(entity.getVin());
        if (!StringUtils.hasText(vin)) {
            throw new RuntimeException("VIN不能为空");
        }
        if (vin.length() != 17) {
            throw new RuntimeException("VIN长度必须为17位");
        }
        if (!vin.matches("^[A-HJ-NPR-Z0-9]{17}$")) {
            throw new RuntimeException("VIN格式不正确");
        }
        entity.setVin(vin);

        String batteryId = entity.getBatteryId() == null ? "" : entity.getBatteryId().trim();
        if (!StringUtils.hasText(batteryId)) {
            throw new RuntimeException("电池ID不能为空");
        }
        entity.setBatteryId(batteryId);

        if (!StringUtils.hasText(entity.getBrand())) {
            throw new RuntimeException("品牌不能为空");
        }
        entity.setBrand(entity.getBrand().trim());

        if (!StringUtils.hasText(entity.getModel())) {
            throw new RuntimeException("型号不能为空");
        }
        entity.setModel(entity.getModel().trim());

        if (entity.getOwnerId() == null) {
            throw new RuntimeException("车主ID不能为空");
        }
        if (entity.getOwnerId() <= 0) {
            throw new RuntimeException("车主ID不合法");
        }

        BatteryInfo battery = batteryInfoService.getById(batteryId);
        if (battery == null) {
            throw new RuntimeException("电池不存在");
        }

        VehicleInfo existingVin = this.getOne(new LambdaQueryWrapper<VehicleInfo>()
                .eq(VehicleInfo::getVin, vin));
        if (existingVin != null) {
            throw new RuntimeException("保存失败：VIN已存在（" + vin + "），请勿重复绑定");
        }

        VehicleInfo existingBattery = this.getOne(new LambdaQueryWrapper<VehicleInfo>()
                .eq(VehicleInfo::getBatteryId, batteryId)
                .orderByDesc(VehicleInfo::getBindTime)
                .last("limit 1"));
        if (existingBattery != null && existingBattery.getVin() != null && !vin.equalsIgnoreCase(existingBattery.getVin())) {
            throw new RuntimeException("该电池已绑定 VIN（" + existingBattery.getVin() + "），如需更换请先解绑/更换记录");
        }

        if (entity.getBindTime() == null) {
            entity.setBindTime(LocalDateTime.now());
        }

        boolean saved = super.save(entity);
        if (saved) {
            String params = JSONUtil.toJsonStr(entity);
            chainService.submitTransaction("bindVehicle", params);
        }
        return saved;
    }
}
