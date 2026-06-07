package com.cxy.aienglishspeakingpracticecompanion.controller;

import com.cxy.aienglishspeakingpracticecompanion.common.BaseResponse;
import com.cxy.aienglishspeakingpracticecompanion.common.ResultUtils;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.LoginRequest;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.LoginResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.RegisterRequest;
import com.cxy.aienglishspeakingpracticecompanion.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /***
     * 注册用户
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody RegisterRequest registerRequest) {
        if (registerRequest.getUsername() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userService.userRegister(registerRequest);
        return ResultUtils.success(userId);
    }

    /***
     *登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        if(loginRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getPassword();
        LoginResponse login = userService.login(userAccount, userPassword,request);
        return ResultUtils.success(login);
    }

    /***
     * 登出请求
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        userService.logout(request);
        return ResultUtils.success(true);
    }
}
