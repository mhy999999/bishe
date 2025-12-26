package com.bishe.service;

import com.bishe.entity.ChainTransaction;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IChainTransactionService extends IService<ChainTransaction> {
    /**
     * 提交数据上链 (模拟)
     * @param methodName 合约方法名
     * @param params 参数JSON
     * @return 交易哈希
     */
    String submitTransaction(String methodName, String params);
}
