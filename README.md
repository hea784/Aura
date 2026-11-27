# Aura — 安全上网空间

Aura 是从 SafeStream 全新重构的 Android 应用。旧的 UI 与功能设计已完全舍弃，采用接近 Apple 现代系统（iOS 18 / Liquid Glass）的设计语言：大圆角、毛玻璃、胶囊分段控件、弹簧动效与清晰的层级排版。

## 核心能力

### 1. 持久化网站空间
- 输入网址后，站点会长期保存在本地数据库（Room），App 重启后依然可用。
- 只有用户手动删除才会移除；支持收藏与"最近打开"排序。
- 每个站点可独立设置视频观看模式：自动 / 短视频 / 普通视频。

### 2. 安全防护（全部本地执行）
- **协议白名单**：仅允许 http / https，拒绝 file、javascript、data、intent 等危险协议。
- **地址校验**：加载前校验 host，拒绝 localhost、环回地址、私有地址、保留地址、多播与云元数据地址（如 169.254.169.254）。
- **跟踪拦截**：内置广告与行为追踪域名名单，拦截相关资源请求。
- **参数清理**：剥离 utm_* 等跟踪参数（精确白名单，绝不误伤 token/sign/auth_key）。
- **Cookie 隔离**：全量禁用 Cookie 与第三方 Cookie，退出即清除。
- **权限拒绝**：摄像头、麦克风、定位、文件选择、弹窗、下载一律拒绝。
- **混合内容禁止**：MIXED_CONTENT_NEVER_ALLOW。
- **安全事件日志**：会话内记录所有拦截事件，可在"防护"页查看。
- **安全通道**：页面到原生只用 `addWebMessageListener`，不使用 `addJavascriptInterface`；所有 WebView 回调切回主线程。

### 3. 双视频观看模式
- **短视频模式**：竖向滚动吸附、沉浸全屏、隐藏滚动条（类似短视频 feed 体验）。
- **普通视频模式**：标准宽屏播放，保留网页原生布局。
- 可在浏览页顶部的分段控件中随时切换，选择会持久化到该站点。

### 4. 画质增强（参考 Kazumi / Anime4K 思路）
- 提供"关闭 / 均衡 / 鲜明"三档。
- 通过注入 CSS filter（对比度、饱和度、亮度）提升观感，并用 ColorMatrix 近似卷积锐化。
- 轻量实现，无需引入 native 着色器依赖。

## 技术栈
- Kotlin 2.0.21 + Jetpack Compose（Material 3）
- Room 2.6.1（KSP）
- AndroidX WebKit（WebMessageListener）
- Coroutines + Flow
- minSdk 26 / targetSdk 35

## 测试
- JVM 单元测试：`UrlGuardTest`、`UrlCleanerTest`、`VisualEnhancementTest`
- 仪器测试：`SecureWebViewInstrumentedTest`（真实 WebView 加固项验证）

## 构建
```bash
./gradlew testDebugUnitTest assembleDebug
```
