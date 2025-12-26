package com.bishe.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.bishe.entity.ChainTransaction;
import com.bishe.mapper.ChainTransactionMapper;
import com.bishe.service.IChainTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class ChainTransactionServiceImpl extends ServiceImpl<ChainTransactionMapper, ChainTransaction> implements IChainTransactionService {

    private static final String MOCK_CONTRACT_ADDR = "0x" + IdUtil.simpleUUID();

    @Override
    public String submitTransaction(String methodName, String params) {
        // 1. 模拟生成交易哈希 (SHA256 of params + timestamp)
        String rawData = methodName + params + System.currentTimeMillis();
        String txHash = "0x" + DigestUtil.sha256Hex(rawData);

        // 2. 构建交易记录
        ChainTransaction tx = new ChainTransaction();
        tx.setTxHash(txHash);
        tx.setBlockHeight(10000L + new Random().nextInt(1000)); // 模拟区块高度
        tx.setContractAddr(MOCK_CONTRACT_ADDR);
        tx.setMethodName(methodName);
        tx.setParams(params);
        tx.setStatus(1); // 1: Success
        tx.setCreateTime(LocalDateTime.now());

        // 3. 保存到数据库
        this.save(tx);

        return txHash;
    }
}
