package com.bishe.service.impl;

import com.bishe.entity.SysUser;
import com.bishe.mapper.SysUserMapper;
import com.bishe.service.ISysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
}
