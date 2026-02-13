# 域名购买详细指南

## 🎯 为家肴App推荐的域名

### 首选方案
```
familyrecipes.app
价格: $20/年
理由: 专为App设计，现代专业
```

### 备选方案
```
1. jiayao.app (家肴拼音，简短好记)
2. familyrecipes.com (经典通用)
3. mykitchen.app (厨房主题)
```

## 📝 购买步骤（Namecheap）

### Step 1: 注册账号
1. 访问 https://www.namecheap.com
2. 点击右上角 "Sign Up"
3. 填写信息：
   ```
   Email: your_email@gmail.com
   Password: (强密码，至少12位)
   ```
4. 验证邮箱

### Step 2: 搜索域名
1. 在首页搜索框输入: `familyrecipes`
2. 选择你想要的后缀（.app 或 .com）
3. 如果显示 "Add to Cart" 说明可以购买

### Step 3: 选择购买年限
```
1年: $20/年（建议先买1年测试）
2年: $39.96 (稍便宜)
5年: $99.90 (提前锁定价格)
```

### Step 4: 添加隐私保护（重要！）
```
WhoisGuard (Free for first year)
✅ 务必勾选！

作用: 隐藏你的个人信息（姓名、地址、电话）
否则: 这些信息会被公开，收到大量垃圾邮件
```

### Step 5: 付款
支持的支付方式：
```
✅ 信用卡（Visa/Mastercard）
✅ PayPal
✅ 支付宝（部分地区）
```

### Step 6: 验证所有权
```
购买后会收到验证邮件
点击链接验证即可
注意: 15天内必须验证，否则域名会被停用
```

## ⚙️ 购买后的DNS配置

### 方案A: 使用Cloudflare（推荐）

#### 1. 添加网站到Cloudflare
```bash
1. 登录 cloudflare.com
2. 点击 "Add a Site"
3. 输入: familyrecipes.app
4. 选择免费计划
```

#### 2. 修改Nameservers
```bash
Cloudflare会提供两个nameserver，例如:
- andy.ns.cloudflare.com
- mary.ns.cloudflare.com

回到Namecheap:
1. 进入域名管理页面
2. Nameservers > Custom DNS
3. 输入Cloudflare提供的两个nameserver
4. 保存

等待时间: 2-48小时（通常10分钟内）
```

#### 3. 在Cloudflare添加DNS记录
```bash
# API服务器
Type: A
Name: api
Content: 你的服务器IP (例如: 143.198.123.456)
Proxy: 开启（橙色云朵）✅

# 网站
Type: A  
Name: www
Content: 你的服务器IP
Proxy: 开启 ✅

# 根域名
Type: A
Name: @
Content: 你的服务器IP  
Proxy: 开启 ✅

# CDN（可选）
Type: CNAME
Name: cdn
Content: api.familyrecipes.app
Proxy: 开启 ✅
```

### 方案B: 直接使用Namecheap DNS

#### 在Namecheap添加DNS记录
```bash
1. 进入域名管理
2. Advanced DNS
3. 添加记录:

Type: A Record
Host: @
Value: 你的服务器IP
TTL: Automatic

Type: A Record  
Host: api
Value: 你的服务器IP
TTL: Automatic

Type: A Record
Host: www
Value: 你的服务器IP
TTL: Automatic
```

## 🔒 SSL证书配置

### 使用Cloudflare（自动免费SSL）
```bash
Cloudflare会自动提供SSL证书，无需额外配置
等待状态变为 "Active Certificate"（约10分钟）
```

### 使用Let's Encrypt（服务器上）
```bash
# 在服务器上执行
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.familyrecipes.app -d www.familyrecipes.app

# 自动续期
sudo crontab -e
# 添加: 0 3 * * * certbot renew --quiet
```

## ✅ 验证配置

### 1. DNS生效检查
```bash
# 本地终端执行
nslookup api.familyrecipes.app

# 应该返回你的服务器IP
```

### 2. HTTPS检查
```bash
# 浏览器访问
https://api.familyrecipes.app

# 应该显示安全锁图标 🔒
```

### 3. API测试
```bash
# 测试健康检查接口
curl https://api.familyrecipes.app/api/health

# 应该返回: {"status":"ok"}
```

## 💰 费用总结

### 最优方案（使用Cloudflare）
```
域名 (familyrecipes.app): $20/年
DNS: Cloudflare免费
CDN: Cloudflare免费  
SSL: Cloudflare免费

总计: $20/年
```

### 标准方案（仅用Namecheap）
```
域名: $20/年
DNS: Namecheap免费
SSL: Let's Encrypt免费

总计: $20/年
```

## 📅 续费提醒

### 重要时间节点
```
购买后30天: 无条件退款期
到期前30天: 续费提醒邮件
到期当天: 域名停用（进入宽限期）
到期后40天: 域名被删除，任何人可注册
```

### 避免域名丢失
```
✅ 开启自动续费（Auto-renew）
✅ 设置日历提醒
✅ 保持邮箱有效
✅ 绑定信用卡/PayPal
```

## 🔄 域名转移（可选）

### 从Namecheap转到Cloudflare
```
时机: 购买60天后可转移
原因: Cloudflare成本价，续费更便宜

步骤:
1. 在Namecheap解锁域名
2. 获取转移码（EPP Code）
3. 在Cloudflare发起转移
4. 确认转移邮件
5. 等待5-7天完成

费用: 免费（包含1年续费）
```

## 🛡️ 安全建议

### 1. 启用两步验证
```
Namecheap账号:
设置 > Security > Two-Factor Authentication
推荐使用: Google Authenticator
```

### 2. 域名锁定
```
防止未授权转移:
域名管理 > Domain Lock > 开启
```

### 3. 隐私保护
```
WhoisGuard必须开启
隐藏个人信息，防止垃圾邮件
```

## 📞 客服支持

### Namecheap
```
Live Chat: 24/7
邮件: support@namecheap.com  
电话: +1 (516) 5680400
响应时间: 5-10分钟
```

### Cloudflare  
```
Community: community.cloudflare.com
免费用户: 仅社区支持
付费用户: 邮件支持
```

## 🎓 学习资源

### DNS基础
- https://www.cloudflare.com/learning/dns/what-is-dns/

### SSL/HTTPS
- https://letsencrypt.org/docs/

### Cloudflare使用
- https://developers.cloudflare.com/

## 🚨 常见问题

### Q1: DNS修改后多久生效？
```
A: 
- Cloudflare: 5-10分钟
- 其他DNS: 2-48小时
- 全球传播: 最多72小时
```

### Q2: 域名忘记续费怎么办？
```
A:
- 到期后30天内: 联系客服，可以恢复
- 30-40天: 进入赎回期，需要额外费用（$100+）  
- 40天后: 域名释放，无法找回
```

### Q3: 可以换域名吗？
```
A: 可以，但建议:
- 同时保留旧域名至少1年
- 设置301重定向
- 更新所有外部链接
```

### Q4: 国内访问需要备案吗？
```
A:
- .app/.com国际域名: 不需要备案（如果服务器不在大陆）
- .cn域名: 必须备案
- 使用Cloudflare CDN: 不需要备案
```

### Q5: 买错了能退款吗？
```
A:
- Namecheap: 购买后30天内可退款（WHOIS未验证）
- 已验证WHOIS: 不能退款
- 建议: 先确认拼写无误再验证
```

## 📋 检查清单

购买域名前：
- [ ] 确认域名拼写正确
- [ ] 检查所有常见后缀是否可用
- [ ] 准备好支付方式（信用卡/PayPal）
- [ ] 准备好邮箱（建议用Gmail）

购买域名后：
- [ ] 验证域名所有权邮件
- [ ] 开启域名锁定
- [ ] 启用WhoisGuard隐私保护
- [ ] 开启账号两步验证
- [ ] 配置DNS记录
- [ ] 测试域名解析
- [ ] 配置SSL证书
- [ ] 测试HTTPS访问
- [ ] 更新Android代码中的API地址
- [ ] 开启自动续费

## 🎯 下一步

域名配置完成后：
1. ✅ 更新 Android 代码中的 BASE_URL
2. ✅ 配置后端 application-prod.yml  
3. ✅ 部署后端到服务器
4. ✅ 测试完整流程
5. ✅ 准备上架 Google Play

---

**推荐操作顺序：**
```
1. 购买域名（今天）
2. 配置Cloudflare（1小时）
3. 等待DNS生效（24小时内）
4. 部署服务器（明天）
5. 测试API（明天）
6. 发布App（下周）
```

祝你的家肴App成功上线！🎉

