package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import lombok.Data;

@Data
public class LoginRequest {
    String password;   // 明文密码（必填）
    String userAccount;
}