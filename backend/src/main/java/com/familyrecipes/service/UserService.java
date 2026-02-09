package com.familyrecipes.service;

import com.familyrecipes.entity.User;
import com.familyrecipes.mapper.UserMapper;
import com.familyrecipes.mapper.ReminderSettingMapper;
import com.familyrecipes.entity.ReminderSetting;
import com.familyrecipes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * 用户服务
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReminderSettingMapper reminderSettingMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @Transactional
    public User register(String email, String password, String username) {
        // 检查邮箱是否已存在
        User existUser = userMapper.findByEmail(email);
        if (existUser != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setEmail(email);
        user.setPassword(hashPassword(password));
        user.setUsername(username);
        userMapper.insert(user);

        // 创建默认提醒设置
        ReminderSetting setting = new ReminderSetting();
        setting.setUserId(user.getId());
        setting.setDaysBeforeExpiry(3);
        setting.setEnabled(true);
        reminderSettingMapper.insert(setting);

        return user;
    }

    /**
     * 用户登录
     */
    public String login(String email, String password) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("邮箱或密码错误");
        }

        if (!user.getPassword().equals(hashPassword(password))) {
            throw new RuntimeException("邮箱或密码错误");
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
        return userMapper.findByEmail(email);
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
        if (!user.getPassword().equals(hashPassword(oldPassword))) {
            throw new RuntimeException("原密码错误");
        }
        userMapper.updatePassword(userId, hashPassword(newPassword));
    }

    /**
     * 密码哈希（简单实现，生产环境建议用BCrypt）
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}

