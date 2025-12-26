package com.bishe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bishe.entity.SalesRecord;
import com.bishe.mapper.SalesRecordMapper;
import com.bishe.service.ISalesRecordService;
import org.springframework.stereotype.Service;

@Service
public class SalesRecordServiceImpl extends ServiceImpl<SalesRecordMapper, SalesRecord> implements ISalesRecordService {
}
