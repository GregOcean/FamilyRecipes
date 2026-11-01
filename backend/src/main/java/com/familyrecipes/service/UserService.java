package com.familyrecipes.service;

import com.familyrecipes.entity.User;
import com.familyrecipes.mapper.UserMapper;
import com.familyrecipes.mapper.ReminderSettingMapper;
import com.familyrecipes.entity.ReminderSetting;
import com.familyrecipes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户服务
 */
@Service
public class UserService {
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReminderSettingMapper reminderSettingMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @Autowired private EmailVerificationService emailCodes;
    @Autowired private KitchenService kitchens;
    public User register(String rawEmail, String password, String username, String code) {
        String email = EmailVerificationService.normalize(rawEmail);
        String passwordHash = hashPassword(password);
        return emailCodes.completeRegistration(email, code, () -> createVerifiedUser(email, passwordHash, username));
    }
    private User createVerifiedUser(String email, String passwordHash, String username) {
        // 检查邮箱是否已存在
        User existUser = userMapper.findByEmail(EmailVerificationService.normalize(email));
        if (existUser != null) {
            throw new com.familyrecipes.common.ApiException(400, "邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setUsername(username);
        userMapper.insert(user);

        // 创建默认提醒设置
        ReminderSetting setting = new ReminderSetting();
        setting.setUserId(user.getId());
        setting.setDaysBeforeExpiry(3);
        setting.setEnabled(true);
        setting.setReminderTime(java.time.LocalTime.of(17, 0));
        reminderSettingMapper.insert(setting);

        // Account, membership and initial kitchen commit together with verification consumption.
        kitchens.list(user.getId());

        return user;
    }

    /**
     * 用户登录
     */
    public String login(String email, String password) {
        User user = userMapper.findByEmail(EmailVerificationService.normalize(email));
        if (user == null) {
            throw new com.familyrecipes.common.ApiException(401, "邮箱或密码错误");
        }

        if (!passwords.matches(password, user.getPassword())) {
            throw new com.familyrecipes.common.ApiException(401, "邮箱或密码错误");
        }

        return jwtUtil.generateToken(user.getId(), user.getEmail());
    }

    /**
     * 获取用户信息
     */
    public User getUserById(Long userId) {
        User user = userMapper.findById(userId);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return user;
    }

    /**
     * 根据邮箱获取用户
     */
    public User getUserByEmail(String email) {
        return userMapper.findByEmail(EmailVerificationService.normalize(email));
    }

    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        userMapper.update(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (!passwords.matches(oldPassword, user.getPassword())) {
            throw new com.familyrecipes.common.ApiException(400, "原密码错误");
        }
        userMapper.updatePassword(userId, hashPassword(newPassword));
    }

    /**
     * 密码哈希：BCrypt，不兼容旧的SHA-256数据
     */
    private String hashPassword(String password) {
        if (password == null || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new com.familyrecipes.common.ApiException(400, "密码至少8位，最多72字节");
        return passwords.encode(password);
    }
}
