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
    public Result<OwnUser> register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getUsername(), request.getCode()
            );
            user.setPassword(null);
            return Result.success(OwnUser.from(user));
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }

    /**
     * 用户登录（支持邮箱或用户名）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
        try {
            String account = request.resolvedAccount();
            var login = userService.loginForAccount(account, request.getPassword());
            String token = login.token();
            User user = login.user();
            user.setPassword(null);
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(OwnUser.from(user));
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
    public Result<OwnUser> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        return Result.success(OwnUser.from(user));
    }

    /**
     * 更新当前用户的资料（用户名、头像）
     */
    @PutMapping("/profile")
    public Result<OwnUser> updateProfile(@jakarta.validation.Valid @RequestBody ProfileRequest request,
                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        User user = userService.updateProfile(userId, request.getUsername(), request.getAvatar());
        return Result.success(OwnUser.from(user));
    }

    @Autowired private com.familyrecipes.service.EmailVerificationService emailCodes;
    @PostMapping("/registration-code")
    public Result<java.util.Map<String,Integer>> sendRegistrationCode(@jakarta.validation.Valid @RequestBody CodeRequest request,
            HttpServletRequest httpRequest) {
        emailCodes.sendRegistrationCode(request.getEmail(), com.familyrecipes.util.ClientAddress.of(httpRequest));
        return Result.success(java.util.Map.of("expires_in",600,"retry_after",60));
    }
    @Autowired private com.familyrecipes.service.PasswordResetService passwordReset;
    @PostMapping("/password-reset-code")
    public Result<?> resetCode(@jakarta.validation.Valid @RequestBody CodeRequest body, HttpServletRequest request) {
        passwordReset.sendCode(body.getEmail(), com.familyrecipes.util.ClientAddress.of(request));
        return Result.success(java.util.Map.of("retry_after",60,"expires_in",600));
    }
    @PostMapping("/password-reset")
    public Result<?> resetPassword(@jakarta.validation.Valid @RequestBody ResetRequest body) {
        passwordReset.reset(body.getEmail(),body.getCode(),body.getPassword());
        return Result.success();
    }
    @Data
    static class ProfileRequest {
        @NotBlank(message = "用户名不能为空")
        @jakarta.validation.constraints.Size(min = 1, max = 50, message = "用户名最多50个字符")
        private String username;

        /** 为空表示不修改头像 */
        @jakarta.validation.constraints.Size(max = 500)
        private String avatar;
    }

    @Data
    static class ResetRequest {
        @NotBlank @Email @jakarta.validation.constraints.Size(max=100) private String email;
        @NotBlank @jakarta.validation.constraints.Pattern(regexp="[0-9]{6}") private String code;
        @NotBlank @jakarta.validation.constraints.Size(min=8,max=72) private String password;
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
        @jakarta.validation.constraints.Size(max = 100)
        private String account;

        /** 兼容旧版客户端和管理后台使用的 email 字段 */
        @jakarta.validation.constraints.Size(max = 100)
        private String email;

        @NotBlank(message = "密码不能为空")
        @jakarta.validation.constraints.Size(min = 8, max = 72, message="密码至少8位，最多72个字符")
        private String password;

        String resolvedAccount() {
            String value = account != null && !account.isBlank() ? account : email;
            if (value == null || value.isBlank()) {
                throw new com.familyrecipes.common.ApiException(400, "账号不能为空");
            }
            return value.trim();
        }
    }

    @Data
    static class LoginResponse {
        private String token;
        private OwnUser user;
    }
    /** Email belongs only in responses for the authenticated account itself. */
    public record OwnUser(Long id,String email,String username,String avatar) {
        static OwnUser from(User user) {return new OwnUser(user.getId(),user.getEmail(),user.getUsername(),user.getAvatar());}
    }
}

