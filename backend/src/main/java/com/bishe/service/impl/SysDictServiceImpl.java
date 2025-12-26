package com.bishe.service.impl;

import com.bishe.entity.SysDict;
import com.bishe.mapper.SysDictMapper;
import com.bishe.service.ISysDictService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements ISysDictService {
}
