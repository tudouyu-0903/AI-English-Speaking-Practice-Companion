package com.cxy.aienglishspeakingpracticecompanion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.model.contant.UserConstant;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.LoginResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.RegisterRequest;
import com.cxy.aienglishspeakingpracticecompanion.model.entity.User;
import com.cxy.aienglishspeakingpracticecompanion.service.SpeakService;
import com.cxy.aienglishspeakingpracticecompanion.mapper.UserMapper;
import com.cxy.aienglishspeakingpracticecompanion.service.UserService;
import com.cxy.aienglishspeakingpracticecompanion.untils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
* @author chenx
* @description 针对表【user(用户信息表)】的数据库操作Service实现
* @createDate 2026-06-06 19:43:15
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String JWT_TOKEN_PREFIX = "jwt:token:";
    private static final long JWT_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /***
     * 注册
     * @param registerRequest
     * @return 用户id
     */
    @Override
    public long userRegister(RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String userAccount = registerRequest.getUserAccount();
        String phone = registerRequest.getPhone();
        String email = registerRequest.getEmail();
        // 1. 校验
        //用户名不能为空不能过长
        if (StrUtil.hasBlank(userAccount,username,password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号过短");
        }
        if (password.length() < 8 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短小于8位");
        }
        //2.判断数据库是否已有数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        //获取数据库查询结果
        Long count = this.baseMapper.selectCount(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        //3.密码加密
        String encryptPassword = getEncryptPassword(password);
        //4. 插入数据
        User appUser = new User();
        appUser.setUsername(username);
        appUser.setUserAccount(userAccount);
        appUser.setPassword(encryptPassword);
        if (phone != null){
            appUser.setPhone(phone);
        }
        if (email != null){
            appUser.setEmail(email);
        }
        //当前时间
        appUser.setUpdate_time( new Date());
        appUser.setCreate_time( new Date());
        boolean save = this.save(appUser);
        if(!save){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存用户失败");
        }
        return appUser.getId();
    }

    /***
     * 用户登录
     * @param userAccount 登录账号
     * @param userPassword 账号密码
     * @param request 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(String userAccount, String userPassword, HttpServletRequest request) {
        //校验
        if(userAccount==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名不能为空");
        }
        if(userPassword==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不能为空");
        }
        //查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //建立查询条件
        queryWrapper.eq("userAccount", userAccount);
        //查询
        User appUser = this.getOne(queryWrapper);
        if (appUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }
        String password = appUser.getPassword();
        if (!password.equals(getEncryptPassword(userPassword))){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名或密码错误");
        }
        //4.保存用户的token并返回
        String token = JwtUtils.generateToken(appUser.getId(), userPassword);
        String key= appUser.getId()+JWT_TOKEN_PREFIX;
        stringRedisTemplate.opsForValue().set(key, token, JWT_EXPIRE_SECONDS, TimeUnit.SECONDS);
        LoginResponse loginResponse = new LoginResponse();
        BeanUtil.copyProperties(appUser, loginResponse);
        loginResponse.setToken(token);
        return loginResponse;
    }

    /***
     * 登出
     * @param request 登出请求
     * @return 登出结果
     */
    @Override
    public boolean logout(HttpServletRequest request) {
        Object userId =request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录");
        }
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /***
     * 获取当前登录用户
     * @param request 请求参数
     * @return 当前用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //1.从request中获取token
        String token = request.getHeader("Authorization");
        if (token != null && StrUtil.startWithIgnoreCase(token, "Bearer ")) {
            token = token.substring("Bearer ".length()).trim();
        }
        if (token==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录");
        }
        Claims claims = JwtUtils.parseToken(token);
        //解析用户ID
        Long userId = claims.get("userId", Long.class);
        User appUser = this.getById(userId);
        //返回当前用户
        return appUser;
    }

    /***
     * 加盐密码
     * @param password
     * @return
     */
    private String getEncryptPassword(String password) {
        // 盐值，混淆密码
        final String SALT = "tudouyu";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }
}




