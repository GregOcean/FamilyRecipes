# Cloudflare 配置完整指南

## 📋 前提条件
- ✅ 已在 Namecheap 购买域名
- ✅ 已验证域名所有权
- ✅ 已注册 Cloudflare 账号

## 🌐 Step 1: 添加网站到 Cloudflare

### 1.1 登录 Cloudflare Dashboard
```
访问: https://dash.cloudflare.com
使用刚注册的邮箱登录
```

### 1.2 添加域名
```bash
1. 点击首页的 "Add a Site" 按钮
2. 输入你的域名: familyrecipes.net (或你购买的其他域名)
3. 点击 "Add site"
```

### 1.3 选择计划
```bash
1. 选择 "Free" 计划 ($0/月)
2. 点击 "Continue"

注意: 免费计划已经完全够用！
```

### 1.4 Cloudflare 扫描现有DNS记录
```bash
1. Cloudflare 会自动扫描 Namecheap 的 DNS 记录
2. 可能显示一些默认记录（停车页面等）
3. 现在先跳过，点击 "Continue"
```

## 🔧 Step 2: 修改 Nameservers（关键步骤）

### 2.1 获取 Cloudflare Nameservers

Cloudflare 会显示两个 Nameserver，类似这样：
```
andy.ns.cloudflare.com
mary.ns.cloudflare.com
```

**重要：记下这两个地址！**

### 2.2 在 Namecheap 修改 Nameservers

```bash
1. 打开新标签页，访问 namecheap.com
2. 登录你的账号
3. 点击左侧 "Domain List"
4. 找到你的域名，点击右侧 "Manage"

5. 找到 "Nameservers" 部分
   默认显示: Namecheap BasicDNS
   
6. 点击下拉菜单，选择 "Custom DNS"

7. 输入 Cloudflare 提供的两个 Nameserver:
   Nameserver 1: andy.ns.cloudflare.com
   Nameserver 2: mary.ns.cloudflare.com
   
8. 点击绿色的 ✓ 保存

9. 会看到提示: "Nameservers updated successfully"
```

### 2.3 回到 Cloudflare 验证

```bash
1. 回到 Cloudflare 标签页
2. 点击 "Done, check nameservers"
3. 等待验证（可能需要几分钟到24小时）

状态检查:
- Pending: 正在等待生效
- Active: 已生效（会收到邮件通知）
```

**耐心等待**: DNS 传播通常需要 10分钟-24小时

## 🎛️ Step 3: 配置 DNS 记录

### 等待 Cloudflare 状态变为 "Active"
```bash
收到邮件: "Your site is active on Cloudflare"
或在 Dashboard 看到绿色 "Active" 标识
```

### 3.1 添加 API 子域名记录

```bash
位置: Cloudflare Dashboard > 你的域名 > DNS > Records

点击 "Add record"，填写：

Record 1: API服务器
-----------------------
Type: A
Name: api
IPv4 address: [暂时留空，等服务器部署后填写]
Proxy status: ✅ Proxied (橙色云朵)
TTL: Auto

例如你的服务器IP是 143.198.123.45，就填这个
```

### 3.2 添加 WWW 记录

```bash
Record 2: 网站主页
-----------------------
Type: A
Name: www
IPv4 address: [同上，与API相同的IP]
Proxy status: ✅ Proxied
TTL: Auto
```

### 3.3 添加根域名记录

```bash
Record 3: 根域名（familyrecipes.net）
-----------------------
Type: A
Name: @
IPv4 address: [同上，与API相同的IP]
Proxy status: ✅ Proxied
TTL: Auto
```

### 3.4 添加下载页面记录（可选）

```bash
Record 4: 下载页面
-----------------------
Type: CNAME
Name: download
Target: www.familyrecipes.net
Proxy status: ✅ Proxied
TTL: Auto
```

### 最终DNS记录示例
```
Type    Name        Content                    Proxy    TTL
──────────────────────────────────────────────────────────
A       api         143.198.123.45            Proxied  Auto
A       www         143.198.123.45            Proxied  Auto
A       @           143.198.123.45            Proxied  Auto
CNAME   download    www.familyrecipes.net     Proxied  Auto
```

## 🔒 Step 4: 配置 SSL/TLS

### 4.1 SSL/TLS 加密模式

```bash
位置: Cloudflare Dashboard > SSL/TLS > Overview

选择加密模式: Full (strict) ✅

模式说明:
- Off: 不加密（❌不推荐）
- Flexible: Cloudflare到客户端加密，Cloudflare到服务器不加密
- Full: 全程加密，但不验证证书
- Full (strict): 全程加密+验证证书（推荐）✅
```

### 4.2 启用 Always Use HTTPS

```bash
位置: SSL/TLS > Edge Certificates

找到 "Always Use HTTPS"
开关: 打开 ✅

效果: http:// 自动重定向到 https://
```

### 4.3 启用 Automatic HTTPS Rewrites

```bash
同一页面继续向下滚动

找到 "Automatic HTTPS Rewrites"
开关: 打开 ✅

效果: 自动将页面中的 http 链接改为 https
```

### 4.4 等待证书生成

```bash
SSL/TLS > Edge Certificates
查看 "Edge Certificates" 部分

状态: 
- Pending Validation: 正在验证（等待5-30分钟）
- Active: 已生效 ✅

证书类型: Universal SSL Certificate (Cloudflare提供)
有效期: 90天（自动续期）
```

## ⚡ Step 5: 性能优化（可选但推荐）

### 5.1 启用 Brotli 压缩

```bash
位置: Speed > Optimization

找到 "Brotli"
开关: 打开 ✅

效果: 压缩文本资源，减少传输大小
```

### 5.2 启用 Auto Minify

```bash
同一页面

找到 "Auto Minify"
勾选:
☑ JavaScript
☑ CSS
☑ HTML

效果: 自动压缩代码，减小文件体积
```

### 5.3 启用 Rocket Loader (可选)

```bash
同一页面

找到 "Rocket Loader"
开关: 打开（可选）

注意: 可能与某些JS库冲突，建议先关闭
如有问题可以在部署后测试
```

### 5.4 配置缓存规则

```bash
位置: Caching > Configuration

Browser Cache TTL: 
选择 "4 hours" 或 "Respect Existing Headers"

Caching Level:
选择 "Standard" ✅
```

## 🛡️ Step 6: 安全设置

### 6.1 启用 Bot Fight Mode (免费防爬虫)

```bash
位置: Security > Bots

找到 "Bot Fight Mode"
开关: 打开 ✅

效果: 自动阻止恶意爬虫
```

### 6.2 Security Level

```bash
位置: Security > Settings

Security Level: Medium ✅ (推荐)

选项说明:
- Essentially Off: 几乎不防护
- Low: 低防护
- Medium: 中等（推荐）✅
- High: 高防护（可能误拦截）
- I'm Under Attack: 极高（仅在被攻击时使用）
```

## ✅ Step 7: 验证配置

### 7.1 检查 DNS 生效

```bash
等待 5-30 分钟后

在本地终端执行:
nslookup api.familyrecipes.net

应该返回 Cloudflare 的 IP（不是你的服务器IP）
这是正常的！因为开启了 Proxy
```

### 7.2 检查 HTTPS

```bash
浏览器访问:
https://www.familyrecipes.net
https://api.familyrecipes.net

如果显示 Cloudflare 默认页面或你的内容 = 成功 ✅
如果显示安全锁图标 🔒 = SSL成功 ✅
```

## 📊 Step 8: 配置分析和监控（可选）

### 8.1 查看流量分析

```bash
位置: Analytics & Logs > Web Analytics

可以看到:
- 请求数量
- 带宽使用
- 缓存命中率
- 威胁拦截
```

### 8.2 设置邮件通知

```bash
位置: Notifications

建议开启:
☑ SSL Certificate Expiration (证书过期提醒)
☑ Hostname SSL Validation Alert (SSL验证警告)
☑ Advanced DDoS Attack (DDoS攻击通知)
```

## 🔄 回到 Namecheap 的最终配置

### 确认 WhoisGuard (隐私保护)

```bash
1. 登录 Namecheap
2. Domain List > Manage
3. 找到 "WhoisGuard" 部分
4. 确保状态: Enabled ✅

如果显示 Disabled:
点击 "Manage" > "Enable"
```

### 设置 Domain Auto-Renew (自动续费)

```bash
同一页面

找到 "Domain" 部分
Auto-Renew: 开启 ✅

重要: 避免忘记续费导致域名丢失
```

## 📝 完成检查清单

配置完成后，检查以下项目：

- [ ] Namecheap 域名已验证
- [ ] Cloudflare 状态显示 Active
- [ ] DNS 记录已添加（api, www, @）
- [ ] SSL 证书状态 Active
- [ ] Always Use HTTPS 已启用
- [ ] Brotli 压缩已启用
- [ ] Auto Minify 已启用
- [ ] Bot Fight Mode 已启用
- [ ] WhoisGuard 已启用
- [ ] Auto-Renew 已启用

## 🎯 下一步

Cloudflare 配置完成后：

1. **等待 DNS 全球传播** (2-24小时)
   - 可以用 https://www.whatsmydns.net 检查

2. **部署后端服务器**
   - 购买 DigitalOcean Droplet
   - 获取服务器 IP
   - 回到 Cloudflare 填写 DNS 记录中的 IP

3. **在服务器上安装 SSL 证书**（后续步骤会详细说明）

4. **更新 Android 代码**
   - 修改 API_BASE_URL
   - 重新编译

5. **测试完整流程**

## 🆘 常见问题

### Q: Cloudflare 一直显示 Pending？
```
A: 
1. 确认 Namecheap Nameserver 已正确修改
2. 等待时间: 通常10分钟-24小时
3. 可以点击 "Recheck now" 按钮
4. 检查邮件，可能已经收到激活通知
```

### Q: DNS 记录中的 IP 地址现在填什么？
```
A:
现在可以先不填，等部署服务器后再填
或者暂时填一个占位 IP: 192.0.2.1
部署服务器后再修改
```

### Q: 橙色云朵是什么意思？
```
A:
橙色云朵 = Proxied = 开启 CDN
灰色云朵 = DNS only = 仅DNS解析

建议保持橙色（Proxied）✅
```

### Q: 配置错了怎么办？
```
A:
Cloudflare 配置随时可以修改
DNS 记录、SSL 设置都可以即时更改
不用担心，试错成本很低
```

### Q: 需要在 Namecheap 也配置 DNS 吗？
```
A:
不需要！
使用 Cloudflare Nameserver 后
所有 DNS 配置都在 Cloudflare 完成
Namecheap 的 DNS 设置会被忽略
```

## 📚 参考资源

- Cloudflare 官方文档: https://developers.cloudflare.com
- DNS 检查工具: https://www.whatsmydns.net
- SSL 测试工具: https://www.ssllabs.com/ssltest/

---

**配置完成时间预估**: 30-60分钟（不含DNS传播等待时间）

完成这些步骤后，你的域名就完全配置好了，可以开始部署服务器了！🚀

