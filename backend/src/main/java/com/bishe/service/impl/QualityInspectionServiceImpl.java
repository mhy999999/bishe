package com.bishe.service.impl;

import cn.hutool.json.JSONUtil;
import com.bishe.entity.QualityInspection;
import com.bishe.mapper.QualityInspectionMapper;
import com.bishe.service.IChainTransactionService;
import com.bishe.service.IQualityInspectionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QualityInspectionServiceImpl extends ServiceImpl<QualityInspectionMapper, QualityInspection> implements IQualityInspectionService {

    @Autowired
    private IChainTransactionService chainService;

    @Override
    public boolean save(QualityInspection entity) {
        if (entity.getCheckTime() == null) {
            entity.setCheckTime(LocalDateTime.now());
        }
        // 上链
        String params = JSONUtil.toJsonStr(entity);
        String txHash = chainService.submitTransaction("recordQualityInspection", params);
        entity.setDataHash(txHash);
        return super.save(entity);
    }
}
