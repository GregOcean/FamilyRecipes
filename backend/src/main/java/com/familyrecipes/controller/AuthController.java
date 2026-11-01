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
    public Result<User> register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getUsername(), request.getCode()
            );
            user.setPassword(null);
            return Result.success(user);
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
        try {
            String token = userService.login(request.getEmail(), request.getPassword());
            User user = userService.getUserByEmail(request.getEmail());
            user.setPassword(null);
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(user);
            return Result.success(response);
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
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

    @Autowired private com.familyrecipes.service.EmailVerificationService emailCodes;
    @PostMapping("/registration-code")
    public Result<java.util.Map<String,Integer>> sendRegistrationCode(@jakarta.validation.Valid @RequestBody CodeRequest request,
            HttpServletRequest httpRequest) {
        emailCodes.sendRegistrationCode(request.getEmail(), httpRequest.getRemoteAddr());
        return Result.success(java.util.Map.of("expires_in",600,"retry_after",60));
    }
    @Data
    static class CodeRequest {
        @NotBlank(message="邮箱不能为空") @Email(message="邮箱格式不正确")
        @jakarta.validation.constraints.Size(max=100,message="邮箱最多100个字符")
        private String email;
    }
    @Data
    static class RegisterRequest {
        @NotBlank(message="请输入邮箱验证码")
        @jakarta.validation.constraints.Pattern(regexp="[0-9]{6}",message="验证码须为6位数字")
        private String code;
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @jakarta.validation.constraints.Size(max = 100)
        private String email;

        @NotBlank(message = "密码不能为空")
        @jakarta.validation.constraints.Size(min = 8, max = 72, message="密码至少8位，最多72个字符")
        private String password;

        @NotBlank(message = "用户名不能为空")
        @jakarta.validation.constraints.Size(max = 50)
        private String username;
    }

    @Data
    static class LoginRequest {
        @NotBlank(message = "邮箱不能为空")
        @jakarta.validation.constraints.Size(max = 100)
        private String email;

        @NotBlank(message = "密码不能为空")
        @jakarta.validation.constraints.Size(min = 8, max = 72, message="密码至少8位，最多72个字符")
        private String password;
    }

    @Data
    static class LoginResponse {
        private String token;
        private User user;
    }
}

