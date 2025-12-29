package com.bishe.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chain_transaction")
public class ChainTransaction {
    @TableId
    private String txHash;
    private Long blockHeight;
    private Long chainId;
    private String fromAddress;
    private String contractAddr;
    private String methodName;
    private String params;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
}
