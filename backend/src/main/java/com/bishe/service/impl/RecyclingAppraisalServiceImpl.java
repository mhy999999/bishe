package com.bishe.service.impl;

import cn.hutool.json.JSONUtil;
import com.bishe.entity.RecyclingAppraisal;
import com.bishe.mapper.RecyclingAppraisalMapper;
import com.bishe.service.IChainTransactionService;
import com.bishe.service.IRecyclingAppraisalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RecyclingAppraisalServiceImpl extends ServiceImpl<RecyclingAppraisalMapper, RecyclingAppraisal> implements IRecyclingAppraisalService {

    @Autowired
    private IChainTransactionService chainService;

    @Override
    public boolean save(RecyclingAppraisal entity) {
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        // 上链
        String params = JSONUtil.toJsonStr(entity);
        String txHash = chainService.submitTransaction("recordRecyclingAppraisal", params);
        entity.setDataHash(txHash);
        return super.save(entity);
    }
}
