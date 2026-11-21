package com.familyrecipes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationMailSender {
    @Autowired private JavaMailSender sender;
    @Value("${app.email.from}") private String from;
    public void send(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("家肴注册验证码");
        message.setText("你的家肴注册验证码是：" + code + "\n\n验证码10分钟内有效，仅可使用一次。重新发送后旧验证码失效。\n如果不是你发起的注册，请忽略此邮件。");
        sender.send(message);
    }
    public void sendPasswordReset(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject("家肴 / Family Recipes — 重置密码 Password reset");
        message.setText("重置密码验证码 / Password reset code: " + code + "\n\n10分钟内有效，仅可使用一次。Valid for 10 minutes, single use.\n如果不是你发起的请求，请忽略。Ignore this email if you did not request it.");
        sender.send(message);
    }
}
