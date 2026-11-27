# Aura 应用代码

## 安全约束（必须与实现一致）
- 仅允许 http/https 请求；加载前校验 host，拒绝 localhost、环回地址、私有地址与保留地址。
- WebView 关闭文件访问、Cookie、第三方 Cookie、定位、弹窗、下载与混合内容。
- 拦截已知跟踪/广告域名；剥离 URL 中的 utm_* 等跟踪参数（精确白名单，避免误剥 token/sign）。
- 页面脚本到原生的通道只用 WebMessageListener，不使用 addJavascriptInterface。
- 所有 WebView 回调切回主线程执行。
