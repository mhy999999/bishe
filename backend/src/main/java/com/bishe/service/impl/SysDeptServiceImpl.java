package com.bishe.service.impl;

import com.bishe.entity.SysDept;
import com.bishe.mapper.SysDeptMapper;
import com.bishe.service.ISysDeptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {
}
