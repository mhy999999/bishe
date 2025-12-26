package com.bishe;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.entity.SysUser;
import com.bishe.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserTest {

    @Autowired
    private ISysUserService sysUserService;

    @Test
    public void testGetAdmin() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, "admin");
        SysUser user = sysUserService.getOne(wrapper);
        if (user != null) {
            System.out.println("User found: " + user.getUsername());
            System.out.println("Password: " + user.getPassword());
        } else {
            System.out.println("User admin not found");
        }
    }
}
