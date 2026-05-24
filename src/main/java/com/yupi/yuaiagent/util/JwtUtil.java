package com.yupi.yuaiagent.util;

import cn.hutool.jwt.JWT;

import java.util.Date;

public class JwtUtil {

    public static String createToken(Long userId, String phone, String secret, long ttl) {
        return JWT.create()
                .setPayload("userId", userId)
                .setPayload("phone", phone)
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl))
                .setKey(secret.getBytes())
                .sign();
    }

    public static boolean verify(String token, String secret) {
        try {
            return JWT.of(token).setKey(secret.getBytes()).verify();
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserId(String token) {
        Object payload = JWT.of(token).getPayload("userId");
        return Long.valueOf(payload.toString());
    }
}
