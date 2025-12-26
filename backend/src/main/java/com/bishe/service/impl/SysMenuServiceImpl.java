package com.bishe.service.impl;

import com.bishe.entity.SysMenu;
import com.bishe.mapper.SysMenuMapper;
import com.bishe.service.ISysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Override
    public Set<String> getPermsByUserId(Long userId) {
        List<String> perms = baseMapper.selectPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (perm != null && !perm.isEmpty()) {
                permsSet.add(perm);
            }
        }
        return permsSet;
    }
}
