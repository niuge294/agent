package com.yupi.yuaiagent.model.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String phone;
    private String password;
}
