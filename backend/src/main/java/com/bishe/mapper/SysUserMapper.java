package com.bishe.mapper;

import com.bishe.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    int deleteUserRolesByUserId(@Param("userId") Long userId);

    int deleteUserRolesByRoleId(@Param("roleId") Long roleId);

    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
