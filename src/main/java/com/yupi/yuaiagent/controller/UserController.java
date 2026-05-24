package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.config.JwtConfig;
import com.yupi.yuaiagent.context.UserContext;
import com.yupi.yuaiagent.model.dto.LoginRequest;
import com.yupi.yuaiagent.model.dto.LoginResponse;
import com.yupi.yuaiagent.model.dto.RegisterRequest;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtConfig jwtConfig;

    public UserController(UserService userService, JwtConfig jwtConfig) {
        this.userService = userService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest request,
                                  HttpServletResponse response) {
        LoginResponse resp = userService.register(request);
        setTokenCookie(response, resp.getToken());
        return resp;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request,
                               HttpServletResponse response) {
        LoginResponse resp = userService.login(request);
        setTokenCookie(response, resp.getToken());
        return resp;
    }

    @GetMapping("/me")
    public LoginResponse me() {
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(jwtConfig.getTokenName(), token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtConfig.getTtl() / 1000));
        response.addCookie(cookie);
    }
}
