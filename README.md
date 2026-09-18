# 红果增强 (HongGuoXposed)

红果免费短剧（国内版 `com.phoenix.read` / 海外版 `com.phoenix.read.oversea.gp`）的 LSPosed 模块，
基于 **libxposed API 102**，使用 **DexKit 2.0** 在运行时按方法名+签名动态查询 Hook 点
（零硬编码混淆类名/方法名），并配以 Jetpack Compose 自定义深色 UI。

> **真实目标来源**：Hook 目标方法名（`isVip` / `canShowPauseAd` / `handleVideoEvent` / `getVipInfo` …）
> 经对 [KEJIYUNB/hongguo](https://github.com/KEJIYUNB/hongguo) 参考项目的逆向分析验证——这些是
> 红果各版本中**稳定未混淆**的业务 API。本模块用 DexKit `findMethod` 按名动态发现这些方法，
> 无需维护任何版本映射表；连 VIP 信息模型类都由 Hook 侧依据反射返回类型动态构造，不出现一个硬编码类名。

## 功能

| 功能 | 说明 | 拦截策略 |
| :--- | :--- | :--- |
| VIP 剧集解锁 | 解锁付费 / VIP 专属短剧 | `isVip`/`isAnyVip`/`canReadShortStory` 等 boolean 方法 → 强制 `true`；`getVipInfo` 系列返回伪造会员模型（到期 2099） |
| 去广告 | 屏蔽暂停广告 / 图标广告 / 片尾广告 | `canShowPauseAd`/`enablePauseAd`/`handleVideoEvent` → 强制 `false`；`requestAd`/`onPauseAdShow` → 置空 |
| 跳过开屏 | 开屏广告立即跳过 | 「可跳过」boolean 方法 → 强制 `true`（best-effort 字符串线索） |
| 解锁下载 | 允许缓存 / 下载付费短剧 | 下载权限 boolean 方法 → 强制 `true`（best-effort 字符串线索） |

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
│   └── scope.list                   #   作用域: com.phoenix.read (+ 海外版)
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
3. 作用域勾选 **红果免费短剧**（`scope.list` 已预置国内版 `com.phoenix.read` + 海外版 `com.phoenix.read.oversea.gp`）；
4. 「强制停止」红果后重新打开；
5. 在模块 UI 的「功能」页按需开关，**修改后需重启红果生效**。

查看 Hook 日志：LSPosed 日志 → 过滤 `HongGuoXposed`。

## Hook 工作流

```
onPackageReady(PackageReadyParam)
  ├── 跳过 WebView 沙箱/渲染进程
  ├── 取 classLoader + applicationInfo.sourceDir(apk 路径)
  ├── FeatureFlags.from(getRemotePreferences("module_config"))   # 读 UI 配置
  ├── DexKitBridge.create(apkPath).use { bridge ->               # 创建一次即关
  │     for (hook in [VIP, 去广告, 跳过开屏, 解锁下载])
  │       if (hook.isEnabled(flags)) hook.apply(bridge, classLoader, flags)
  │           └── Finders.findXxx(bridge, classLoader)           # DexKit 查询
  │           └── bridge.findMethod { matcher { name="isVip"; ... } }   # 按方法名精确查询
  │           └── MethodData.getMethodInstance(classLoader)      # 解析为反射 Method
  │           └── 仅保留返回类型匹配的方法（boolean/void 过滤）
  │           └── xp.hook(method).setExceptionMode(PROTECTIVE).intercept { ... }
  └── }
```

## 如何精调 Hook 点（关键）

所有查询点集中在 **`app/src/main/kotlin/com/hg/xposed/dexkit/Finders.kt`**，分两类：

**1. 稳定方法名（VIP/广告核心，跨版本无需改）：**
```kotlin
val VIP_BOOLEAN_NAMES = listOf("isVip","isAnyVip","canReadShortStory","hasVipShortSeriesPrivilege",
    "hasNoAdFollAllScene","hasNoAdForShortSeries","isVipUser","isSpecificVipOrHigher","canShowVipCenter")
val AD_BOOLEAN_NAMES = listOf("canShowPauseAd","enablePauseAd","handleVideoEvent")
val AD_VOID_NAMES = listOf("requestAd","onPauseAdShow")
```
DexKit `findMethod` 在 `com.dragon.read.*`（红果业务代码包前缀）范围内按名精确匹配，
命中后按反射 `Method.returnType` 过滤为 boolean/void，再安装拦截器。

**2. best-effort 字符串线索（开屏/下载，混淆概率高）：**
```kotlin
val SPLASH_BOOLEAN_KEYWORDS = listOf("canSkip","isSkip","skipEnable","canJump","skipAd")
val DOWNLOAD_BOOLEAN_KEYWORDS = listOf("canDownload","allowDownload","isDownloadEnable","downloadEnable")
```
对新版本用 jadx / frida-trace 定位到新方法名后，**只改这一处常量列表**即可，无需动任何 Hook 逻辑。
解析为反射 `Method` 失败会自动跳过并告警，不会崩溃（`module.prop` 已设 `exceptionMode=protective`）。

## 免责声明

本模块仅供学习与研究用途，请勿用于商业或非法用途。使用本模块带来的任何后果由使用者自行承担。
