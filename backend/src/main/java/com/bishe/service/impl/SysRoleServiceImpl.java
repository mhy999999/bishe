package com.bishe.service.impl;

import com.bishe.entity.SysRole;
import com.bishe.mapper.SysRoleMapper;
import com.bishe.service.ISysRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    
    @Override
    public Set<String> getRoleKeysByUserId(Long userId) {
        List<String> keys = baseMapper.selectRoleKeysByUserId(userId);
        return new HashSet<>(keys);
    }
}
