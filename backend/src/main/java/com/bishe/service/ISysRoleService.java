package com.bishe.service;

import com.bishe.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.Set;

public interface ISysRoleService extends IService<SysRole> {
    /**
     * 根据用户ID获取角色权限集合
     * @param userId 用户ID
     * @return 角色权限集合
     */
    Set<String> getRoleKeysByUserId(Long userId);
}
