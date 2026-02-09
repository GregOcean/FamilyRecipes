package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.entity.User;
import com.familyrecipes.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getUsername()
            );
            user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            String token = userService.login(request.getEmail(), request.getPassword());
            User user = userService.getUserByEmail(request.getEmail());
            user.setPassword(null);
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(user);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    @Data
    static class RegisterRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "密码不能为空")
        private String password;

        @NotBlank(message = "用户名不能为空")
        private String username;
    }

    @Data
    static class LoginRequest {
        @NotBlank(message = "邮箱不能为空")
        private String email;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    static class LoginResponse {
        private String token;
        private User user;
    }
}

