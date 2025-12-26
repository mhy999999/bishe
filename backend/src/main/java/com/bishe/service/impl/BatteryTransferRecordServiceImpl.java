package com.bishe.service.impl;

import cn.hutool.json.JSONUtil;
import com.bishe.entity.BatteryTransferRecord;
import com.bishe.mapper.BatteryTransferRecordMapper;
import com.bishe.service.IBatteryTransferRecordService;
import com.bishe.service.IChainTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BatteryTransferRecordServiceImpl extends ServiceImpl<BatteryTransferRecordMapper, BatteryTransferRecord> implements IBatteryTransferRecordService {

    @Autowired
    private IChainTransactionService chainService;

    @Override
    public boolean save(BatteryTransferRecord entity) {
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        // 上链
        String params = JSONUtil.toJsonStr(entity);
        String txHash = chainService.submitTransaction("recordTransfer", params);
        entity.setTxHash(txHash);
        return super.save(entity);
    }
}
