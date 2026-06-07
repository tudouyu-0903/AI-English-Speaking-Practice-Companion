package com.cxy.aienglishspeakingpracticecompanion.aop;

import cn.hutool.core.util.StrUtil;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.model.entity.User;
import com.cxy.aienglishspeakingpracticecompanion.service.UserService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

@Aspect
@Component
public class AuthInterceptor  {
    private static final String JWT_TOKEN_PREFIX = "jwt:token:";
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //获取当前HttpServletRequest
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String token = request.getHeader("Authorization");
        if (token != null && StrUtil.startWithIgnoreCase(token, "Bearer ")) {
            token = token.substring("Bearer ".length()).trim();
        }
        //获取当前用户
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long id = loginUser.getId();
        String key=id+JWT_TOKEN_PREFIX;
        //查redis中是否存在对应token
        String redisToken = stringRedisTemplate.opsForValue().get(key);
        if (!token.equals(redisToken)){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return joinPoint.proceed();
    }
}
