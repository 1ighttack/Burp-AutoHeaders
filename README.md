# AutoHeaders

Burp Suite 自动注入 HTTP Header 的插件（X-Forwarded-For、X-Real-IP 等），基于 Montoya API。

> Author: 1ight

## 功能

- **一键开关** — Burp 顶部标签页直接控制，无需右键菜单
- **10 个预设 Header** — X-Forwarded-For、X-Real-IP、X-Forwarded-Proto、X-Forwarded-Host、Forwarded、CF-Connecting-IP、True-Client-IP、X-Client-IP、X-Requested-With、X-Api-Version
- **自定义 Header** — 通过 "Add Header" 添加任意 Header
- **随机公网 IP** — 每个请求使用不同的随机 IP（默认）
- **自定义固定值** — 设置一个固定的 Header 值
- **单独控制** — 每个 Header 独立勾选开关
- **覆盖模式** — 请求中已存在的同名 Header 会被覆盖，不会重复
- **验证功能** — 一键发送测试请求到 httpbin.org，确认 Header 已注入

<!-- 截图位置 -->
<!-- ![panel](images/panel.png) -->

## 环境要求

- Burp Suite Professional / Community（2024.x+，支持 Montoya API）
- Java 22+

## 安装

**方式一：直接下载**

从 [Releases](../../releases) 下载 `AutoHeaders.jar`，在 Burp 中加载：`Extensions > Installed > Add`

**方式二：源码编译**

```bash
git clone https://github.com/你的用户名/AutoHeaders.git
cd AutoHeaders
mvn clean package
```

加载 `target/AutoHeaders-1.1.jar` 即可。

## 使用方法

1. 加载插件后，Burp 顶部出现 **AutoHeaders** 标签页
2. 点击 **Random IP** 或 **Custom Value** 启用（状态变为 ON）
3. 按需勾选/取消各个 Header
4. 点 **Verify** 验证 Header 是否注入成功
5. **只要开关为 ON，所有经过 Proxy 的请求都会自动注入选定的 Header，无需其他操作**

## 原理

插件注册 `ProxyRequestHandler`，拦截经过 Burp Proxy 的每个请求。启用后在原始 HTTP 请求的 `\r\n\r\n` 处插入配置的 Header 和 IP 值，再发送到目标服务器。

## 注意事项

**CDN 会剥离常见 Header**

大多数 CDN（如 Cloudflare、阿里云 CDN 等）和 WAF 会在转发请求时主动删除 `X-Forwarded-For`、`X-Real-IP`、`X-Forwarded-Proto` 等常见 IP 伪造 Header，以防止客户端篡改。插件注入的 Header 确实发送到了 CDN 节点，但 CDN 不会将其转发到源站。如果发现某个 Header 在服务端未生效，尝试使用 CDN 不会剥离的 Header，如 `X-Api-Version`、`X-Client-IP`、`True-Client-IP` 等。可以通过 **Verify** 按钮测试哪些 Header 能穿透到目标。

**Proxy History 显示的是原始请求**

由于 Burp 的架构限制，请求在进入 Proxy History 时就已经被记录，此时插件的注入尚未发生，因此 History 中看到的始终是未修改的原始请求。这不是插件问题。

**Repeater 发送方式差异**

- **右键菜单** → `Send to Repeater`：发送的是注入 Header 后的请求，Repeater 中可以看到并使用这些 Header
- **快捷键 Ctrl+R**：发送的是 History 中的原始请求（未注入 Header），因为快捷键走的是 History 记录的原始数据

如果需要在 Repeater 中使用注入后的请求，**请通过右键菜单发送**。

## 许可

MIT
