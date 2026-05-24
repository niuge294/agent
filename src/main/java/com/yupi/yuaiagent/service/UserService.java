package com.yupi.yuaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuaiagent.model.dto.LoginRequest;
import com.yupi.yuaiagent.model.dto.LoginResponse;
import com.yupi.yuaiagent.model.dto.RegisterRequest;
import com.yupi.yuaiagent.model.entity.User;

public interface UserService extends IService<User> {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
