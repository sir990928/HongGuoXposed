# 红果增强 (HongGuoXposed)

红果免费短剧（`com.phoenix.read`）的 LSPosed 模块，基于 **libxposed API 102**，
使用 **DexKit 2.0** 在运行时动态查询 Hook 点（零硬编码类名/方法名），
并配以 Jetpack Compose 自定义深色 UI。

## 功能

| 功能 | 说明 | 拦截策略 |
| :--- | :--- | :--- |
| VIP 剧集解锁 | 解锁付费 / VIP 专属短剧 | 会员校验 `boolean` 方法 → 强制返回 `true`；付费墙触发 `void` 方法 → 置空 |
| 去广告 | 屏蔽信息流 / 播放页 / 弹窗广告 | 广告加载/展示 `void` 方法 → 置空（原方法体跳过） |
| 跳过开屏 | 开屏广告立即跳过 | 「可跳过」`boolean` 方法 → 强制返回 `true` |
| 解锁下载 | 允许缓存 / 下载付费短剧 | 下载权限 `boolean` 方法 → 强制返回 `true` |

## 技术栈

- **入口**：`io.github.libxposed:api:102.0.0`（现代 libxposed 模块 API，继承 `XposedModule`）
- **Hook 点查询**：`org.luckypray:dexkit:2.0.0`（运行时 dex 反混淆查询）
- **UI**：Jetpack Compose + Material 3（自定义深色品牌主题）
- **配置同步**：UI 写 `SharedPreferences("module_config")` ↔ Hook 读 `getRemotePreferences("module_config")`

## 目录结构

```
app/src/main/
├── AndroidManifest.xml              # 模块声明 + xposedscope
├── resources/META-INF/xposed/       # libxposed 102 现代模块元数据
│   ├── module.prop                  #   minApiVersion=102 / targetApiVersion=102
│   ├── java_init.list               #   入口类: com.hg.xposed.MainHook
│   └── scope.list                   #   作用域: com.phoenix.read
└── kotlin/com/hg/xposed/
    ├── MainHook.kt                  # 模块入口：onModuleLoaded / onPackageReady
    ├── core/                        # Target 常量 / Logger / FeatureFlags
    ├── dexkit/                      # DexKitProvider + Finders（查询中枢）
    ├── hooks/                       # BaseHook + 4 个功能 Hook
    └── ui/                          # Compose 主题 / 组件 / 三屏 / ViewModel
```

## 构建

需要 JDK 17+、Android SDK（platform 34、build-tools 34）。

```bash
# 1. 配置 SDK 路径（任选其一）
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
#   或 export ANDROID_HOME=/path/to/Android/Sdk

# 2. 构建 Debug APK
./gradlew :app:assembleDebug

# 产物
# app/build/outputs/apk/debug/app-debug.apk
```

> 用 Android Studio 打开本项目亦可，IDE 会自动下载 Gradle 与 SDK。

## 使用

1. 安装生成的 APK；
2. 在 **LSPosed** 中启用本模块；
3. 作用域勾选 **红果免费短剧（`com.phoenix.read`）**（`scope.list` 已预置默认作用域）；
4. 「强制停止」红果后重新打开；
5. 在模块 UI 的「功能」页按需开关，**修改后需重启红果生效**。

查看 Hook 日志：LSPosed 日志 → 过滤 `HongGuoXposed`。

## Hook 工作流

```
onPackageReady(PackageReadyParam)
  ├── 取 classLoader + applicationInfo.sourceDir(apk 路径)
  ├── FeatureFlags.from(getRemotePreferences("module_config"))   # 读 UI 配置
  ├── DexKitBridge.create(apkPath).use { bridge ->               # 创建一次即关
  │     for (hook in [VIP, 去广告, 跳过开屏, 解锁下载])
  │       if (hook.isEnabled(flags)) hook.apply(bridge, classLoader, flags)
  │           └── Finders.findXxx(bridge, classLoader)           # DexKit 查询
  │           └── bridge.findMethod { matcher { addUsingString(kw); returnType=... } }
  │           └── MethodData.getMethodInstance(classLoader)      # 解析为反射 Method
  │           └── xp.hook(method).intercept { ... }              # 安装拦截器
  └── }
```

## 如何精调 Hook 点（关键）

红果为字节跳动应用，类名高度混淆且随版本变化。本模块的所有查询点集中在
**`app/src/main/kotlin/com/hg/xposed/dexkit/Finders.kt` → `Hints`**，用 jadx / frida-trace
定位到新的高信号字符串后，**只需改这一处**，无需动任何 Hook 逻辑：

```kotlin
object Hints {
    var vipBooleanKeywords    = listOf("is_vip", "isVip", "vipStatus", ...)
    var paywallVoidKeywords   = listOf("开通会员", "立即开通", "解锁全集", ...)
    var adVoidKeywords       = listOf("loadAd", "load_ad", "广告", "csj", "pangle", ...)
    var splashBooleanKeywords = listOf("canSkip", "can_skip", "canJump", ...)
    var downloadBooleanKeywords = listOf("canDownload", "allow_download", ...)
}
```

查询语义：对每个关键字在 `com.phoenix / com.bytedance / com.ss.android` 包范围内做
**包含匹配**，并叠加 `returnType`（`boolean` / `void`）约束，命中后按方法 descriptor 去重。
解析为反射 `Method` 失败会自动跳过并告警，不会崩溃（`module.prop` 已设 `exceptionMode=protective`）。

## 免责声明

本模块仅供学习与研究用途，请勿用于商业或非法用途。使用本模块带来的任何后果由使用者自行承担。
