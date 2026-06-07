package com.cxy.aienglishspeakingpracticecompanion.untils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {
    // 密钥（建议使用更安全的随机字符串，此处沿用 "Y3h5" 保持一致性）
    private static final String SECRET_KEY = "tudouyu";

    // 过期时间：12小时（单位毫秒）
    private static final long EXPIRATION = 12 * 60 * 60 * 1000; // 12小时

    /**
     * 生成 JWT 令牌
     *
     * @param
     * @return JWT 字符串
     */
    public static String generateToken(Long userId,String passWord) {
        Map<String, Object> claims=new HashMap<>();
        claims.put("userId",userId);
        claims.put("passWord",passWord);
        return Jwts.builder()
                .setClaims(claims) // 设置自定义内容
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION)) // 过期时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY) // 签名算法和密钥
                .compact();
    }

    /**
     * 解析 JWT 令牌
     *
     * @param token JWT 令牌字符串
     * @return Claims 包含解析出的数据
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY) // 使用相同密钥解析
                .parseClaimsJws(token)
                .getBody();
    }

}
