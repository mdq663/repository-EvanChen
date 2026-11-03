# 悦跑湖工本地密码锁说明

## 初始密码

当前构建的测试密码：`GoGoGo@2026`

密码只以 PBKDF2 校验摘要编译进 APK，不以明文保存到源码常量、
`SharedPreferences` 或应用日志中。

## 修改密码

密码摘要位于：

`app/src/main/java/com/zcshou/gogogo/PasswordConfig.java`

在工程根目录使用 JDK 17 执行：

```powershell
java tools/PasswordVerifierGenerator.java "新的统一密码"
```

命令会输出 64 位十六进制摘要。将 `PasswordConfig.java` 中的
`PASSWORD_VERIFIER` 替换为该摘要，保持 `PASSWORD_SALT` 和
`PBKDF2_ITERATIONS` 不变。

重新编译并发布新 APK 后，旧授权记录会因为保存的摘要与当前摘要不一致而
自动失效，用户需要重新输入新密码。

## 构建环境

- JDK 17
- Android SDK Platform 32
- Android Build Tools 36.0.0
- 工程自带 Gradle Wrapper 8.13

Android Studio 打开工程后，先确认 `compileSdk`、Build Tools 和 JDK 17
已配置，然后执行：

```powershell
.\gradlew.bat assembleRelease
```

如果 Windows 构建目录包含中文，AAPT2 可能报告资源或 `android.jar`
无法读取。此时请把工程复制到纯英文路径后再构建。

## APK 安装

- 支持 Android 8.0（API 27）及以上
- 当前 APK 仅包含 `arm64-v8a`，适用于 64 位 ARM Android 手机
- 如果手机上已有不同签名、同包名的版本，需要先卸载旧版本
- 允许文件管理器或聊天软件“安装未知应用”后，点击 APK 安装

首次安装后启动应用，会立即显示密码验证页面。输入正确密码并通过原应用
所需的权限、网络与 GPS 检查后，会进入原欢迎页并自动继续到原主界面。

## 验收测试

1. 首次安装后打开应用，必须显示“欢迎使用”和密码输入页。
2. 输入错误密码，显示“密码错误，请重新输入。”且不能进入原主界面。
3. 输入正确密码，显示“验证成功，正在进入应用。”并进入原应用。
4. 退出应用后重新打开，不再要求输入密码。
5. 在系统设置中清除本应用数据，再次打开必须重新输入密码。
6. 卸载并重新安装，再次打开必须重新输入密码。
7. 依次测试地图、历史记录、路线管理、路线绘制、路线模拟和设置页面。

## 已实现的安全路径

- `AuthActivity` 是唯一桌面启动入口
- `MainActivity`、原欢迎页、历史、路线、设置等页面都有授权复查
- 所有受保护页面均不是外部导出组件
- 从后台服务通知进入主界面时，同样会触发授权检查
- 授权状态和当前密码摘要保存在应用私有 `SharedPreferences`
- 应用备份已禁用，卸载重装不会从系统备份恢复授权状态
- 密码比较使用 PBKDF2-HMAC-SHA256 和恒定时间摘要比较
- 密码页禁止系统截屏，不打印密码
- 已停用从上游仓库自动更新，避免上游安装包覆盖本地密码锁

## 本机构建验证记录

- `:app:assembleRelease`：成功
- R8 压缩和 Release 签名：成功
- 密码逻辑单元测试：3 项通过（正确密码、错误密码、空密码）
- APK 签名校验：APK Signature Scheme v2 通过
- ZIP 对齐校验：通过
- APK 启动入口：`com.zcshou.gogogo.AuthActivity`
- APK 包名：`com.zcshou.gogogo`
- APK 应用名：`悦跑湖工`
- APK 版本：`1.12.4`（versionCode 1124）
- APK SHA-256：
  `0B0D735B52439F7ABAD7E078931F0513CE88DBA60927B6EA285DEECB3C583C5A`
- APK 的 `classes.dex` 中未检索到明文测试密码

已在 vivo V2463A（Android 17、arm64-v8a）上完成以下实机验证：

- 全新安装后启动应用，停留在 `AuthActivity` 密码页
- 错误密码无法进入主界面
- 正确密码可进入 `MainActivity`
- 强制停止后重新打开，直接进入原主界面，无需重复输入密码
- 清除应用数据后再次打开，重新显示密码页
- 卸载并重新安装后，重新显示密码页
- 主界面和百度地图底图可以正常显示

手机日志中百度地图 SDK 返回“APP 服务被禁用”。该提示来自源码自带的
百度地图 AK，没有被密码锁修改；如果位置搜索或逆地理编码受限，需要在
百度开放平台为包名 `com.zcshou.gogogo` 和当前签名配置有效 AK。
