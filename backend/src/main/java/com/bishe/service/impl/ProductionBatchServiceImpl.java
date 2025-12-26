package com.bishe.service.impl;

import com.bishe.entity.ProductionBatch;
import com.bishe.mapper.ProductionBatchMapper;
import com.bishe.service.IProductionBatchService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductionBatchServiceImpl extends ServiceImpl<ProductionBatchMapper, ProductionBatch> implements IProductionBatchService {
}
