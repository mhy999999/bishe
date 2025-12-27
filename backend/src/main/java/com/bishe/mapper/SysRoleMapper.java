package com.bishe.mapper;

import com.bishe.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    
    /**
     * 根据用户ID查询角色权限字符串
     * @param userId 用户ID
     * @return 角色Key列表
     */
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    int deleteRoleMenusByRoleId(@Param("roleId") Long roleId);

    int deleteRoleMenusByMenuId(@Param("menuId") Long menuId);

    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
