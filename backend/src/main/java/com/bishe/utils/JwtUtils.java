package com.bishe.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    // 密钥，生产环境应从配置文件读取
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // 24小时过期
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * 生成Token
     * @param userId 用户ID
     * @param username 用户名
     * @param deptId 部门ID
     * @return Token字符串
     */
    public String generateToken(Long userId, String username, Long deptId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        if (deptId != null) {
            claims.put("deptId", deptId);
        }
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析Token获取Claims
     * @param token Token字符串
     * @return Claims对象
     */
    public Claims getClaimsByToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证Token是否有效
     * @param token Token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        return getClaimsByToken(token) != null;
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = getClaimsByToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }
}
