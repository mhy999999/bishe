package com.bishe.service;

import com.bishe.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.Set;

public interface ISysMenuService extends IService<SysMenu> {
    /**
     * 根据用户ID获取权限集合
     * @param userId 用户ID
     * @return 权限集合
     */
    Set<String> getPermsByUserId(Long userId);
}
