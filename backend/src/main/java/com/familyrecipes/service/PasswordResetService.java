package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Durable single-instance challenge/rate limits; no plaintext codes in DB or logs. */
@Service
public class PasswordResetService {
    @Autowired private JdbcTemplate db;
    @Autowired private TransactionTemplate transactions;
    @Autowired private VerificationMailSender mail;
    @Autowired private Clock clock;
    @Value("${app.email.code-secret}") private String secret;
    private final SecureRandom random = new SecureRandom();
    public static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String hash(String purpose, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((purpose + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    public void sendCode(String rawEmail, String address) {
        String email = normalize(rawEmail);
        long now = clock.millis();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String digest = hash("password-reset", email + ":" + code);
        String addressHash = hash("address", address);
        // Reserve before SMTP, so failed/slow deliveries still consume the rate limit.
        transactions.executeWithoutResult(status -> {
            db.update("INSERT IGNORE INTO registration_email_rate(address_hash,window_start) VALUES(?,?)", addressHash, now);
            var rate = db.queryForMap("SELECT * FROM registration_email_rate WHERE address_hash=? FOR UPDATE", addressHash);
            long start = ((Number) rate.get("window_start")).longValue();
            int count = now - start >= 3600000 ? 0 : ((Number) rate.get("send_count")).intValue();
            if (count >= 20) throw new ApiException(429, "发送过于频繁，请一小时后再试");
            db.update("INSERT IGNORE INTO password_reset_code(email) VALUES(?)", email);
            var challenge = db.queryForMap("SELECT * FROM password_reset_code WHERE email=? FOR UPDATE", email);
            if (now - ((Number) challenge.get("issued_at")).longValue() < 60000)
                throw new ApiException(429, "验证码每60秒只能发送一次，请稍后再试");
            long emailStart = ((Number) challenge.get("window_start")).longValue();
            int emailCount = now - emailStart >= 3600000 ? 0 : ((Number) challenge.get("send_count")).intValue();
            if (emailCount >= 5) throw new ApiException(429, "此邮箱每小时最多发送5次，请稍后再试");
            db.update("UPDATE registration_email_rate SET window_start=?,send_count=? WHERE address_hash=?", count == 0 ? now : start, count + 1, addressHash);
            db.update("UPDATE password_reset_code SET code_hash=?,issued_at=?,expires_at=?,window_start=?,send_count=?,attempts=0,active=FALSE,consumed=FALSE WHERE email=?",
                digest, now, now + 600000, emailCount == 0 ? now : emailStart, emailCount + 1, email);
        });
        try {
            if (db.queryForObject("SELECT COUNT(*) FROM user WHERE email=?", Integer.class, email) == 0) return;
            mail.sendPasswordReset(email, code);
            db.update("UPDATE password_reset_code SET active=TRUE WHERE email=? AND code_hash=? AND consumed=FALSE", email, digest);
        } catch (org.springframework.mail.MailException e) {
            db.update("UPDATE password_reset_code SET issued_at=0,active=FALSE WHERE email=? AND code_hash=?", email, digest);
            throw new ApiException(503, "验证码邮件发送失败，请稍后重试");
        }
    }
    public void reset(String email, String code, String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "密码至少8位，UTF-8编码不得超过72字节");
        String encoded = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
        completeReset(email, code, () -> {
            var users = db.queryForList("SELECT id FROM user WHERE email=? FOR UPDATE", normalize(email));
            if (users.isEmpty()) throw new ApiException(400, "验证码已失效，请重新获取");
            long id=((Number)users.get(0).get("id")).longValue();
            db.update("UPDATE user SET password=? WHERE id=?", encoded, id);
            db.update("INSERT INTO user_auth_version(user_id,version) VALUES(?,1) ON DUPLICATE KEY UPDATE version=version+1", id);
            return true;
        });
    }
    private record ResetResult<T>(T value, String error) { }
    public <T> T completeReset(String rawEmail, String code, Supplier<T> createAccount) {
        String email = normalize(rawEmail);
        ResetResult<T> result = transactions.execute(status -> {
            var rows = db.queryForList("SELECT * FROM password_reset_code WHERE email=? FOR UPDATE", email);
            if (rows.isEmpty()) return new ResetResult<T>(null, "请先获取邮箱验证码");
            var row = rows.get(0);
            if (!Boolean.TRUE.equals(row.get("active")) || Boolean.TRUE.equals(row.get("consumed")) ||
                clock.millis() >= ((Number) row.get("expires_at")).longValue())
                return new ResetResult<T>(null, "验证码已失效，请重新获取");
            int attempts = ((Number) row.get("attempts")).intValue();
            if (attempts >= 5) return new ResetResult<T>(null, "验证码错误次数过多，请重新获取");
            String actual = hash("password-reset", email + ":" + code);
            if (code == null || !code.matches("[0-9]{6}") || !MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                row.get("code_hash").toString().getBytes(StandardCharsets.UTF_8))) {
                db.update("UPDATE password_reset_code SET attempts=attempts+1 WHERE email=?", email);
                return new ResetResult<T>(null, attempts == 4 ? "验证码错误次数过多，请重新获取" : "验证码不正确");
            }
            T account = createAccount.get();
            db.update("UPDATE password_reset_code SET consumed=TRUE,active=FALSE WHERE email=?", email);
            return new ResetResult<>(account, null);
        });
        // Throw after commit: incorrect attempts must survive HTTP request failure.
        if (result.error() != null) throw new ApiException(400, result.error());
        return result.value();
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=3600000)
    public void cleanExpiredChallenges() {
        long cutoff = clock.millis() - 86400000;
        db.update("DELETE FROM password_reset_code WHERE expires_at < ? AND issued_at < ?", cutoff, cutoff);
        db.update("DELETE FROM registration_email_rate WHERE window_start < ?", cutoff);
    }
}
