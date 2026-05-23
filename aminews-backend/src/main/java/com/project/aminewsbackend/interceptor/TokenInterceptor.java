package com.project.aminewsbackend.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.aminewsbackend.entity.User;
import com.project.aminewsbackend.mapper.UserMapper;
import com.project.aminewsbackend.service.RedisService;
import com.project.aminewsbackend.utils.JwtUtil;
import com.project.aminewsbackend.utils.UserThreadLocal;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getMethod().equals(RequestMethod.OPTIONS.name())) {
            System.out.println("[TokenInterceptor] 处理OPTIONS预检请求，跳过token校验。");
            return true; // 直接放行 OPTIONS 请求
        }

        String token = request.getHeader("Authorization");
        System.out.println("[TokenInterceptor] 请求的token为: " + token);
        if (StringUtils.isBlank(token) || token.length() < 8) {
            System.out.println("[TokenInterceptor] 请求头中缺少或格式错误的token: " + token);
            throw new Exception("请求头中缺少token");
        }
        token = token.substring(7); // 去掉 "Bearer " 前缀

        try {
            Object userObj = redisService.get("user:token:" + token);
            System.out.println("[TokenInterceptor] 从Redis中获取的用户信息: " + userObj);
            // 重置token过期时间
            redisService.set("user:token:" + token, userObj, 1, TimeUnit.DAYS);
            if (userObj == null) {
                System.out.println("[TokenInterceptor] Redis中不存在该用户信息，token: " + token);
                throw new Exception("不存在的用户");
            }
            User user;
            if (userObj instanceof User) {
                user = (User) userObj;
            } else {
                user = objectMapper.convertValue(userObj, User.class);
            }
            System.out.println("[TokenInterceptor] 用户信息已放入ThreadLocal, 用户ID: " + user.getId());
            UserThreadLocal.set(user);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("[TokenInterceptor] Token已过期: " + e.getMessage());
            throw new Exception("token已过期");
        } catch (Exception e) {
            System.out.println("[TokenInterceptor] 获取用户信息失败: " + e.getMessage());
            throw new Exception("获取用户信息失败");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //程序运行结束之后，删除线程，防止内存泄漏
        UserThreadLocal.remove();
    }
}
