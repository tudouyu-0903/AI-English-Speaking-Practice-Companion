package com.cxy.aienglishspeakingpracticecompanion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.LoginResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.RegisterRequest;
import com.cxy.aienglishspeakingpracticecompanion.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
    /***
     *用户注册
     * @param registerRequest
     * @return
     */
    long userRegister(RegisterRequest registerRequest);

    /***
     * 用户登录
     * @param userAccount 登录账号
     * @param userPassword 账号密码
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(String userAccount, String userPassword, HttpServletRequest request);

    /***
     * 登出
     * @param request 登出请求
     * @return 登出结果
     */
    boolean logout(HttpServletRequest request);

    /***
     * 获取当前登录用户
     * @param request 请求参数
     * @return 当前用户
     */
    User getLoginUser(HttpServletRequest request);
}