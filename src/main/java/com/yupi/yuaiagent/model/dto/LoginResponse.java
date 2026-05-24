package com.yupi.yuaiagent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String username;
    private String phone;
    private String role;

    @JsonIgnore
    private String token;
}
