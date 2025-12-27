package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.dto.LoginDto;
import com.bishe.entity.*;
import com.bishe.mapper.SysRoleMapper;
import com.bishe.mapper.SysUserMapper;
import com.bishe.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return sysRoleService.getRoleKeysByUserId(userId).contains("admin");
    }

    private <T> Result<T> forbidden() {
        return Result.error(403, "无权限");
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码处理失败");
        }
    }

    private String hashPassword(String plain) {
        String salt = java.util.UUID.randomUUID().toString().replace("-", "");
        String hash = sha256Hex(salt + plain);
        return "sha256$" + salt + "$" + hash;
    }

    private boolean verifyPassword(String stored, String plain) {
        if (!StringUtils.hasText(stored) || plain == null) {
            return false;
        }
        if (stored.startsWith("sha256$")) {
            String[] parts = stored.split("\\$");
            if (parts.length != 3) {
                return false;
            }
            String salt = parts[1];
            String hash = parts[2];
            return sha256Hex(salt + plain).equalsIgnoreCase(hash);
        }
        return stored.equals(plain);
    }

    // ==================== 用户管理 (SysUser) ====================

    @PostMapping("/user/login")
    public Result<java.util.Map<String, Object>> login(@RequestBody LoginDto loginDto) {
        if (!StringUtils.hasText(loginDto.getUsername()) || !StringUtils.hasText(loginDto.getPassword())) {
            return Result.error(400, "用户名或密码不能为空");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDto.getUsername());
        SysUser user = sysUserService.getOne(wrapper);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            return Result.error(403, "账号已被禁用");
        }
        String storedPassword = user.getPassword();
        if (!verifyPassword(storedPassword, loginDto.getPassword())) {
            return Result.error("密码错误");
        }

        // 生成Token
        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername(), user.getDeptId());
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("token", token);
        user.setPassword(null);
        map.put("user", user);

        if (StringUtils.hasText(storedPassword) && !storedPassword.startsWith("sha256$")) {
            SysUser toUpdate = new SysUser();
            toUpdate.setUserId(user.getUserId());
            toUpdate.setPassword(hashPassword(loginDto.getPassword()));
            toUpdate.setUpdateTime(java.time.LocalDateTime.now());
            sysUserService.updateById(toUpdate);
        }

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
        user.setPassword(null);
        
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
        if (!StringUtils.hasText(sysUser.getUsername()) || !StringUtils.hasText(sysUser.getPassword())) {
            return Result.error(400, "用户名或密码不能为空");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, sysUser.getUsername());
        if (sysUserService.count(wrapper) > 0) {
            return Result.error("用户名已存在");
        }
        sysUser.setCreateTime(java.time.LocalDateTime.now());
        sysUser.setUpdateTime(java.time.LocalDateTime.now());
        if (!sysUser.getPassword().startsWith("sha256$")) {
            sysUser.setPassword(hashPassword(sysUser.getPassword()));
        }
        if (sysUser.getStatus() == null) {
            sysUser.setStatus(0);
        }
        return Result.success(sysUserService.save(sysUser));
    }

    @GetMapping("/user/list")
    public Result<Page<UserListVo>> listUser(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String username,
                                             HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username);
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserService.page(page, wrapper);

        java.util.List<SysUser> users = result == null || result.getRecords() == null ? java.util.List.of() : result.getRecords();

        java.util.Set<Long> deptIds = new java.util.HashSet<>();
        java.util.List<Long> userIds = new java.util.ArrayList<>();
        for (SysUser u : users) {
            if (u == null) {
                continue;
            }
            if (u.getUserId() != null) {
                userIds.add(u.getUserId());
            }
            if (u.getDeptId() != null) {
                deptIds.add(u.getDeptId());
            }
        }

        java.util.Map<Long, String> deptNameById = new java.util.HashMap<>();
        if (!deptIds.isEmpty()) {
            LambdaQueryWrapper<SysDept> deptWrapper = new LambdaQueryWrapper<>();
            deptWrapper.in(SysDept::getDeptId, deptIds);
            java.util.List<SysDept> depts = sysDeptService.list(deptWrapper);
            if (depts != null) {
                for (SysDept d : depts) {
                    if (d != null && d.getDeptId() != null) {
                        deptNameById.put(d.getDeptId(), d.getDeptName());
                    }
                }
            }
        }

        java.util.Map<Long, java.util.List<String>> roleNamesByUserId = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            java.util.List<java.util.Map<String, Object>> roleRows = sysUserMapper.selectRoleNamesByUserIds(userIds);
            if (roleRows != null) {
                for (java.util.Map<String, Object> row : roleRows) {
                    if (row == null) {
                        continue;
                    }
                    Object userIdObj = row.get("userId");
                    Object roleNameObj = row.get("roleName");
                    if (!(userIdObj instanceof Number) || roleNameObj == null) {
                        continue;
                    }
                    Long uid = ((Number) userIdObj).longValue();
                    String roleName = String.valueOf(roleNameObj);
                    roleNamesByUserId.computeIfAbsent(uid, k -> new java.util.ArrayList<>()).add(roleName);
                }
            }
        }

        java.util.List<UserListVo> voList = new java.util.ArrayList<>();
        for (SysUser u : users) {
            if (u == null) {
                continue;
            }
            UserListVo vo = new UserListVo();
            vo.setUserId(u.getUserId());
            vo.setDeptId(u.getDeptId());
            vo.setDeptName(u.getDeptId() == null ? null : deptNameById.get(u.getDeptId()));
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setEmail(u.getEmail());
            vo.setPhone(u.getPhone());
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());
            vo.setUpdateTime(u.getUpdateTime());
            vo.setChainAccount(u.getChainAccount());
            vo.setRoleNames(roleNamesByUserId.getOrDefault(u.getUserId(), java.util.List.of()));
            voList.add(vo);
        }

        Page<UserListVo> out = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        out.setRecords(voList);
        return Result.success(out);
    }

    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping("/user")
    public Result<SysUser> saveUser(@RequestBody SysUser sysUser, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        if (!StringUtils.hasText(sysUser.getUsername())) {
            return Result.error(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(sysUser.getPassword())) {
            return Result.error(400, "密码不能为空");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, sysUser.getUsername());
        if (sysUserService.count(wrapper) > 0) {
            return Result.error(400, "用户名已存在");
        }
        sysUser.setCreateTime(java.time.LocalDateTime.now());
        sysUser.setUpdateTime(java.time.LocalDateTime.now());
        if (!sysUser.getPassword().startsWith("sha256$")) {
            sysUser.setPassword(hashPassword(sysUser.getPassword()));
        }
        if (sysUser.getStatus() == null) {
            sysUser.setStatus(0);
        }
        boolean saved = sysUserService.save(sysUser);
        if (!saved) {
            return Result.error("创建失败");
        }
        sysUser.setPassword(null);
        return Result.success(sysUser);
    }

    @PutMapping("/user")
    public Result<Boolean> updateUser(@RequestBody SysUser sysUser, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysUser.setUpdateTime(java.time.LocalDateTime.now());
        if (StringUtils.hasText(sysUser.getPassword()) && !sysUser.getPassword().startsWith("sha256$")) {
            sysUser.setPassword(hashPassword(sysUser.getPassword()));
        } else if (!StringUtils.hasText(sysUser.getPassword())) {
            sysUser.setPassword(null);
        }
        return Result.success(sysUserService.updateById(sysUser));
    }

    @DeleteMapping("/user/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> removeUser(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysUserMapper.deleteUserRolesByUserId(id);
        return Result.success(sysUserService.removeById(id));
    }

    @GetMapping("/user/{id}/roleIds")
    public Result<List<Long>> getUserRoleIds(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysUserMapper.selectRoleIdsByUserId(id));
    }

    @PutMapping("/user/{id}/roleIds")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateUserRoleIds(@PathVariable Long id, @RequestBody List<Long> roleIds, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysUserMapper.deleteUserRolesByUserId(id);
        if (roleIds != null && !roleIds.isEmpty()) {
            sysUserMapper.insertUserRoles(id, roleIds);
        }
        return Result.success(true);
    }

    // ==================== 角色管理 (SysRole) ====================

    @GetMapping("/role/list")
    public Result<Page<SysRole>> listRole(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String roleName,
                                          HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName);
        return Result.success(sysRoleService.page(page, wrapper));
    }

    @GetMapping("/role/all")
    public Result<List<SysRole>> listAllRole(HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRole::getRoleId);
        return Result.success(sysRoleService.list(wrapper));
    }

    @GetMapping("/role/{id}")
    public Result<SysRole> getRoleById(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysRoleService.getById(id));
    }

    @PostMapping("/role")
    public Result<SysRole> saveRole(@RequestBody SysRole sysRole, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysRole.setCreateTime(java.time.LocalDateTime.now());
        if (sysRole.getStatus() == null) {
            sysRole.setStatus(0);
        }
        boolean saved = sysRoleService.save(sysRole);
        if (!saved) {
            return Result.error("创建失败");
        }
        return Result.success(sysRole);
    }

    @PutMapping("/role")
    public Result<Boolean> updateRole(@RequestBody SysRole sysRole, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysRoleService.updateById(sysRole));
    }

    @DeleteMapping("/role/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> removeRole(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysRoleMapper.deleteRoleMenusByRoleId(id);
        sysUserMapper.deleteUserRolesByRoleId(id);
        return Result.success(sysRoleService.removeById(id));
    }

    @GetMapping("/role/{id}/menuIds")
    public Result<List<Long>> getRoleMenuIds(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysRoleMapper.selectMenuIdsByRoleId(id));
    }

    @PutMapping("/role/{id}/menuIds")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateRoleMenuIds(@PathVariable Long id, @RequestBody List<Long> menuIds, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysRoleMapper.deleteRoleMenusByRoleId(id);
        if (menuIds != null && !menuIds.isEmpty()) {
            sysRoleMapper.insertRoleMenus(id, menuIds);
        }
        return Result.success(true);
    }

    // ==================== 部门管理 (SysDept) ====================

    @GetMapping("/dept/list")
    public Result<List<SysDept>> listDept(HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        // 部门通常是树形结构，这里简单返回列表，前端处理树形
        return Result.success(sysDeptService.list());
    }

    @GetMapping("/dept/tree")
    public Result<List<DeptNode>> treeDept(HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        List<SysDept> list = sysDeptService.list();
        return Result.success(buildDeptTree(list));
    }

    @GetMapping("/dept/{id}")
    public Result<SysDept> getDeptById(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDeptService.getById(id));
    }

    @PostMapping("/dept")
    public Result<Boolean> saveDept(@RequestBody SysDept sysDept, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDeptService.save(sysDept));
    }

    @PutMapping("/dept")
    public Result<Boolean> updateDept(@RequestBody SysDept sysDept, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDeptService.updateById(sysDept));
    }

    @DeleteMapping("/dept/{id}")
    public Result<Boolean> removeDept(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDeptService.removeById(id));
    }

    // ==================== 菜单管理 (SysMenu) ====================

    @GetMapping("/menu/list")
    public Result<List<SysMenu>> listMenu(HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysMenuService.list());
    }

    @GetMapping("/menu/tree")
    public Result<List<MenuNode>> treeMenu(HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getOrderNum);
        List<SysMenu> list = sysMenuService.list(wrapper);
        return Result.success(buildMenuTree(list));
    }

    @PostMapping("/menu")
    public Result<Boolean> saveMenu(@RequestBody SysMenu sysMenu, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        if (sysMenu.getParentId() == null) {
            sysMenu.setParentId(0L);
        }
        return Result.success(sysMenuService.save(sysMenu));
    }

    @PutMapping("/menu")
    public Result<Boolean> updateMenu(@RequestBody SysMenu sysMenu, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysMenuService.updateById(sysMenu));
    }

    @DeleteMapping("/menu/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> removeMenu(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        sysRoleMapper.deleteRoleMenusByMenuId(id);
        return Result.success(sysMenuService.removeById(id));
    }

    // ==================== 字典管理 (SysDict) ====================

    @GetMapping("/dict/list")
    public Result<Page<SysDict>> listDict(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String dictType,
                                          HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        Page<SysDict> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dictType), SysDict::getDictType, dictType);
        return Result.success(sysDictService.page(page, wrapper));
    }

    @PostMapping("/dict")
    public Result<Boolean> saveDict(@RequestBody SysDict sysDict, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDictService.save(sysDict));
    }

    @PutMapping("/dict")
    public Result<Boolean> updateDict(@RequestBody SysDict sysDict, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDictService.updateById(sysDict));
    }

    @DeleteMapping("/dict/{id}")
    public Result<Boolean> removeDict(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(getCurrentUserId(request))) {
            return forbidden();
        }
        return Result.success(sysDictService.removeById(id));
    }

    @lombok.Data
    public static class MenuNode extends SysMenu {
        private java.util.List<MenuNode> children;
    }

    @lombok.Data
    public static class DeptNode extends SysDept {
        private java.util.List<DeptNode> children;
    }

    @lombok.Data
    public static class UserListVo {
        private Long userId;
        private Long deptId;
        private String deptName;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private Integer status;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
        private String chainAccount;
        private java.util.List<String> roleNames;
    }

    private List<MenuNode> buildMenuTree(List<SysMenu> menus) {
        java.util.Map<Long, MenuNode> map = new java.util.HashMap<>();
        for (SysMenu menu : menus) {
            MenuNode node = new MenuNode();
            node.setMenuId(menu.getMenuId());
            node.setMenuName(menu.getMenuName());
            node.setParentId(menu.getParentId());
            node.setOrderNum(menu.getOrderNum());
            node.setPath(menu.getPath());
            node.setComponent(menu.getComponent());
            node.setPerms(menu.getPerms());
            node.setMenuType(menu.getMenuType());
            node.setChildren(new java.util.ArrayList<>());
            map.put(node.getMenuId(), node);
        }

        java.util.List<MenuNode> roots = new java.util.ArrayList<>();
        for (MenuNode node : map.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !map.containsKey(parentId)) {
                roots.add(node);
            } else {
                map.get(parentId).getChildren().add(node);
            }
        }
        roots.sort(java.util.Comparator.comparingInt(n -> n.getOrderNum() == null ? 0 : n.getOrderNum()));
        for (MenuNode root : roots) {
            sortMenuChildren(root);
        }
        return roots;
    }

    private void sortMenuChildren(MenuNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }
        node.getChildren().sort(java.util.Comparator.comparingInt(n -> n.getOrderNum() == null ? 0 : n.getOrderNum()));
        for (MenuNode child : node.getChildren()) {
            sortMenuChildren(child);
        }
    }

    private List<DeptNode> buildDeptTree(List<SysDept> depts) {
        java.util.Map<Long, DeptNode> map = new java.util.HashMap<>();
        for (SysDept dept : depts) {
            DeptNode node = new DeptNode();
            node.setDeptId(dept.getDeptId());
            node.setParentId(dept.getParentId());
            node.setDeptName(dept.getDeptName());
            node.setOrderNum(dept.getOrderNum());
            node.setLeader(dept.getLeader());
            node.setPhone(dept.getPhone());
            node.setStatus(dept.getStatus());
            node.setChildren(new java.util.ArrayList<>());
            map.put(node.getDeptId(), node);
        }
        java.util.List<DeptNode> roots = new java.util.ArrayList<>();
        for (DeptNode node : map.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !map.containsKey(parentId)) {
                roots.add(node);
            } else {
                map.get(parentId).getChildren().add(node);
            }
        }
        roots.sort(java.util.Comparator.comparingInt(n -> n.getOrderNum() == null ? 0 : n.getOrderNum()));
        for (DeptNode root : roots) {
            sortDeptChildren(root);
        }
        return roots;
    }

    private void sortDeptChildren(DeptNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }
        node.getChildren().sort(java.util.Comparator.comparingInt(n -> n.getOrderNum() == null ? 0 : n.getOrderNum()));
        for (DeptNode child : node.getChildren()) {
            sortDeptChildren(child);
        }
    }
}
