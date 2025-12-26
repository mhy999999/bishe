package com.bishe.service.impl;

import cn.hutool.json.JSONUtil;
import com.bishe.entity.MaintenanceRecord;
import com.bishe.mapper.MaintenanceRecordMapper;
import com.bishe.service.IChainTransactionService;
import com.bishe.service.IMaintenanceRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MaintenanceRecordServiceImpl extends ServiceImpl<MaintenanceRecordMapper, MaintenanceRecord> implements IMaintenanceRecordService {

    @Autowired
    private IChainTransactionService chainService;

    @Override
    public boolean save(MaintenanceRecord entity) {
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        // 上链
        String params = JSONUtil.toJsonStr(entity);
        String txHash = chainService.submitTransaction("recordMaintenance", params);
        entity.setTxHash(txHash);
        return super.save(entity);
    }
}
