package com.yupi.yuaiagent.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuaiagent.config.JwtConfig;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.mapper.UserMapper;
import com.yupi.yuaiagent.model.dto.LoginRequest;
import com.yupi.yuaiagent.model.dto.LoginResponse;
import com.yupi.yuaiagent.model.dto.RegisterRequest;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.service.UserService;
import com.yupi.yuaiagent.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtConfig jwtConfig;

    public UserServiceImpl(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone());
        if (exists(query)) {
            throw new BusinessException("手机号已注册");
        }
        User user = new User();
        user.setUsername("user_" + RandomUtil.randomString(6));
        user.setPhone(request.getPhone());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setRole("USER");
        user.setStatus(1);
        save(user);

        User saved = getOne(query);
        String token = JwtUtil.createToken(saved.getId(), saved.getPhone(),
                jwtConfig.getSecret(), jwtConfig.getTtl());
        return toResponse(saved, token);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException("手机号未注册");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        String token = JwtUtil.createToken(user.getId(), user.getPhone(),
                jwtConfig.getSecret(), jwtConfig.getTtl());
        return toResponse(user, token);
    }

    private LoginResponse toResponse(User user, String token) {
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
