package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.dto.LoginDto;
import com.bishe.entity.*;
import com.bishe.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理统一控制器
 * 包含: 用户管理, 角色管理, 部门管理, 菜单管理, 字典管理
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysRoleService sysRoleService;
    @Autowired
    private ISysDeptService sysDeptService;
    @Autowired
    private ISysMenuService sysMenuService;
    @Autowired
    private ISysDictService sysDictService;
    @Autowired
    private com.bishe.utils.JwtUtils jwtUtils;

    // ==================== 用户管理 (SysUser) ====================

    @PostMapping("/user/login")
    public Result<java.util.Map<String, Object>> login(@RequestBody LoginDto loginDto) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDto.getUsername());
        SysUser user = sysUserService.getOne(wrapper);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!user.getPassword().equals(loginDto.getPassword())) {
            return Result.error("密码错误");
        }
        
        // 生成Token
        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername(), user.getDeptId());
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("token", token);
        map.put("user", user);
        
        return Result.success(map);
    }

    @GetMapping("/user/info")
    public Result<java.util.Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("Token无效");
        }
        
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("user", user);
        
        // Fetch roles and permissions
        java.util.Set<String> roles = sysRoleService.getRoleKeysByUserId(userId);
        java.util.Set<String> permissions = sysMenuService.getPermsByUserId(userId);
        
        // If admin, give all permissions (convention)
        if (roles.contains("admin")) {
            permissions.add("*:*:*");
        }
        
        data.put("roles", roles);
        data.put("permissions", permissions);
        data.put("name", user.getNickname());
        data.put("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        
        return Result.success(data);
    }

    @PostMapping("/user/logout")
    public Result<String> logout() {
        return Result.success("退出成功");
    }

    @PostMapping("/user/register")
    public Result<Boolean> register(@RequestBody SysUser sysUser) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, sysUser.getUsername());
        if (sysUserService.count(wrapper) > 0) {
            return Result.error("用户名已存在");
        }
        sysUser.setCreateTime(java.time.LocalDateTime.now());
        return Result.success(sysUserService.save(sysUser));
    }

    @GetMapping("/user/list")
    public Result<Page<SysUser>> listUser(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String username) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username);
        wrapper.orderByDesc(SysUser::getCreateTime);
        return Result.success(sysUserService.page(page, wrapper));
    }

    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @PostMapping("/user")
    public Result<Boolean> saveUser(@RequestBody SysUser sysUser) {
        return Result.success(sysUserService.save(sysUser));
    }

    @PutMapping("/user")
    public Result<Boolean> updateUser(@RequestBody SysUser sysUser) {
        return Result.success(sysUserService.updateById(sysUser));
    }

    @DeleteMapping("/user/{id}")
    public Result<Boolean> removeUser(@PathVariable Long id) {
        return Result.success(sysUserService.removeById(id));
    }

    // ==================== 角色管理 (SysRole) ====================

    @GetMapping("/role/list")
    public Result<Page<SysRole>> listRole(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String roleName) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName);
        return Result.success(sysRoleService.page(page, wrapper));
    }

    @GetMapping("/role/{id}")
    public Result<SysRole> getRoleById(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
    }

    @PostMapping("/role")
    public Result<Boolean> saveRole(@RequestBody SysRole sysRole) {
        return Result.success(sysRoleService.save(sysRole));
    }

    @PutMapping("/role")
    public Result<Boolean> updateRole(@RequestBody SysRole sysRole) {
        return Result.success(sysRoleService.updateById(sysRole));
    }

    @DeleteMapping("/role/{id}")
    public Result<Boolean> removeRole(@PathVariable Long id) {
        return Result.success(sysRoleService.removeById(id));
    }

    // ==================== 部门管理 (SysDept) ====================

    @GetMapping("/dept/list")
    public Result<List<SysDept>> listDept() {
        // 部门通常是树形结构，这里简单返回列表，前端处理树形
        return Result.success(sysDeptService.list());
    }

    @GetMapping("/dept/{id}")
    public Result<SysDept> getDeptById(@PathVariable Long id) {
        return Result.success(sysDeptService.getById(id));
    }

    @PostMapping("/dept")
    public Result<Boolean> saveDept(@RequestBody SysDept sysDept) {
        return Result.success(sysDeptService.save(sysDept));
    }

    @PutMapping("/dept")
    public Result<Boolean> updateDept(@RequestBody SysDept sysDept) {
        return Result.success(sysDeptService.updateById(sysDept));
    }

    @DeleteMapping("/dept/{id}")
    public Result<Boolean> removeDept(@PathVariable Long id) {
        return Result.success(sysDeptService.removeById(id));
    }

    // ==================== 菜单管理 (SysMenu) ====================

    @GetMapping("/menu/list")
    public Result<List<SysMenu>> listMenu() {
        return Result.success(sysMenuService.list());
    }

    @PostMapping("/menu")
    public Result<Boolean> saveMenu(@RequestBody SysMenu sysMenu) {
        return Result.success(sysMenuService.save(sysMenu));
    }

    @PutMapping("/menu")
    public Result<Boolean> updateMenu(@RequestBody SysMenu sysMenu) {
        return Result.success(sysMenuService.updateById(sysMenu));
    }

    @DeleteMapping("/menu/{id}")
    public Result<Boolean> removeMenu(@PathVariable Long id) {
        return Result.success(sysMenuService.removeById(id));
    }

    // ==================== 字典管理 (SysDict) ====================

    @GetMapping("/dict/list")
    public Result<Page<SysDict>> listDict(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String dictType) {
        Page<SysDict> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dictType), SysDict::getDictType, dictType);
        return Result.success(sysDictService.page(page, wrapper));
    }

    @PostMapping("/dict")
    public Result<Boolean> saveDict(@RequestBody SysDict sysDict) {
        return Result.success(sysDictService.save(sysDict));
    }

    @PutMapping("/dict")
    public Result<Boolean> updateDict(@RequestBody SysDict sysDict) {
        return Result.success(sysDictService.updateById(sysDict));
    }

    @DeleteMapping("/dict/{id}")
    public Result<Boolean> removeDict(@PathVariable Long id) {
        return Result.success(sysDictService.removeById(id));
    }
}
