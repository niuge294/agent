package com.yupi.yuaiagent.interceptor;

import com.yupi.yuaiagent.config.JwtConfig;
import com.yupi.yuaiagent.context.UserContext;
import com.yupi.yuaiagent.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final JwtConfig jwtConfig;

    public UserContextInterceptor(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Cookie[] cookies = request.getCookies();
        log.info("请求 {} 携带 Cookie 数量: {}", request.getRequestURI(),
                cookies == null ? 0 : cookies.length);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (jwtConfig.getTokenName().equals(cookie.getName())) {
                    if (JwtUtil.verify(cookie.getValue(), jwtConfig.getSecret())) {
                        UserContext.setUserId(JwtUtil.getUserId(cookie.getValue()));
                        log.info("用户 {} 已认证", UserContext.getUserId());
                    }
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
