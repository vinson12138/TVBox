# AGENTS.md — TVBox (FongMi/TV)

本文件为 AI Agent 在此代码库中工作提供指导。

---

## 语言规范

**所有回复和文档均须使用中文。** 包括代码注释、提交信息说明、技术分析、错误排查建议等一切文字输出，均以中文书写。

---

## 项目概述

基于 **CatVod** 可插拔爬虫框架构建的开源 Android 媒体中心，同时支持电视端（leanback）和手机端（mobile）。通过外部 JSON 配置文件加载点播内容和直播频道，爬虫（Spider）在运行时以 JAR、JavaScript 或 Python 插件形式动态加载。

**关键参数：** minSdk 24，targetSdk 28，compileSdk 36，Java 17，版本号 5.2.6。

---

## 模块结构

```
/app       主应用（leanback + mobile 风味，arm64_v8a + armeabi_v7a ABI）
/catvod    核心库：Spider 基类、OkHttp 网络栈、公共 Bean/工具类
/quickjs   QuickJS JS 引擎封装（运行 JS 爬虫）
/chaquo    Chaquopy Python 3.10 引擎（运行 Python 爬虫）
```

- `app/src/main/` — 共享代码（Bean、API 配置、播放器、数据库、事件、服务器、工具类）
- `app/src/leanback/` — TV 端 UI（Activity、Adapter、Presenter、Dialog）
- `app/src/mobile/` — 手机端 UI（Activity、Adapter、Dialog、Fragment）

---

## 架构

采用 **MVVM** 模式，通过 EventBus 进行跨组件通信。

- `ViewModel`（`SiteViewModel`、`LiveViewModel`）持有 `MutableLiveData<Result>`，由 Activity 观察。
- **GreenRobot EventBus 3.3.1** — 事件类型：`ConfigEvent`、`PlayerEvent`、`ErrorEvent`、`ActionEvent`、`RefreshEvent`、`CastEvent`、`ServerEvent`、`ScanEvent`、`StateEvent`。
- 所有单例均使用**按需初始化持有者**模式（`private static class Loader { static volatile X INSTANCE = ... }`），无 DI 框架。
- 全局使用 `ViewBinding`，禁止 `findViewById`。

### 并发模型

| 线程池 | 大小 | 用途 |
|--------|------|------|
| `App.executor` | 5 线程 | 通用：点播、配置、播放器 |
| `App.searchExecutor` | 20 线程 | 多站点并行搜索 |
| `LiveViewModel` executor | 2 线程 | 直播任务（4 个类型槽） |
| 主线程 | — | `App.post(r, delay)` 通过 Handler 调度 |

播放超时：启动播放 15 秒，URL 解析 15 秒。

---

## 数据流

### 点播内容

```
用户选择站点
  → SiteViewModel.homeContent()
  → App.submit()（后台线程）
  → Spider.homeContent() [JAR / JS / PY] 或 OkHttp 直接请求
  → Result.fromJson()
  → MutableLiveData<Result>.postValue()
  → Activity 观察者 → RecyclerView 刷新
```

### 播放流程

```
用户点击集数
  → SiteViewModel.playerContent()
  → Spider.playerContent() → Result（url + parse 标志）
  → Source.get().fetch(result) [路由特殊协议：magnet、thunder、youtube、strm …]
  → Players.start(result)
  → ParseJob.start()（parse==1 时）→ JSON API / WebView 嗅探 / JAR 扩展 / 万能解析
  → ParseJob.onParseSuccess() → Players.setMediaItem()
  → ExoPlayer 缓冲并播放
```

### 配置加载

```
VodConfig.load(config, callback)
  → Decoder.getJson(url)  [明文 JSON | Base64 "**" 前缀 | AES-CBC 十六进制 "2423" 前缀]
  → parseConfig() → initSite/initParse/initLive/initWall
  → BaseLoader.get().parseJar(spider)  [DexClassLoader 预加载 JAR]
  → EventBus: ConfigEvent.vod() → UI 刷新
```

---

## 关键类速查

| 类名 | 位置 | 职责 |
|------|------|------|
| `App` | `main/.../App.java` | Application 单例，提供全局 Gson、线程池、主线程 Handler、`Hook` |
| `VodConfig` | `main/.../api/config/VodConfig.java` | 解析点播配置 JSON，管理当前活跃站点和解析器 |
| `LiveConfig` | `main/.../api/config/LiveConfig.java` | 解析直播配置（JSON/M3U/TXT），管理频道 |
| `Decoder` | `main/.../api/Decoder.java` | 拉取配置 URL，解码 Base64/AES-CBC |
| `BaseLoader` | `main/.../api/loader/BaseLoader.java` | 路由爬虫初始化：JS→JsLoader，PY→PyLoader，JAR→JarLoader |
| `Spider` | `catvod/.../crawler/Spider.java` | 所有爬虫继承的抽象基类 |
| `SiteViewModel` | `main/.../model/SiteViewModel.java` | 点播数据：首页、分类、详情、播放、搜索 |
| `LiveViewModel` | `main/.../model/LiveViewModel.java` | 直播数据：频道列表、EPG、流地址解析 |
| `Players` | `main/.../player/Players.java` | ExoPlayer 封装，处理 DRM、轨道选择、弹幕、重试 |
| `Source` | `main/.../player/Source.java` | 将 URL 路由到对应 Extractor（Thunder、YouTube 等） |
| `ParseJob` | `main/.../player/ParseJob.java` | URL 解析任务，支持 5 种解析类型（0–4） |
| `Server` / `Nano` | `main/.../server/` | NanoHTTPD 本地 HTTP API（端口 9978–9998） |
| `AppDatabase` | `main/.../db/AppDatabase.java` | Room 数据库（版本 35）：Keep、Site、Live、Track、Config、Device、History |
| `Setting` | `main/.../Setting.java` | 所有 SharedPreferences 的静态访问器 |

---

## 站点类型码

| 类型 | 含义 |
|------|------|
| `0` | XML API（旧版 `videolist`/`detail` 接口，XML 响应） |
| `1` | JSON API v1 |
| `3` | JAR 爬虫（API 以 `csp_` 为前缀） |
| `4` | JS 爬虫（API 以 `.js` 结尾） |

## 解析类型码

| 类型 | 含义 |
|------|------|
| `0` | WebView 嗅探（正则拦截媒体 URL） |
| `1` | JSON API（HTTP 请求 `parse_url + video_url`） |
| `2` | JSON 扩展（通过 JAR `jsonExt` 并行调用所有 type-1 解析器） |
| `3` | JSON 混合（通过 JAR `jsonExtMix` 的 flag 感知多解析器） |
| `4` | 万能解析（所有 type-1 解析器 + WebView 嗅探并行执行） |

---

## Bean / 模型规范

- `app/src/main/.../bean/` — 所有 Gson 序列化模型。**禁止重命名或混淆字段**（ProGuard 保留 `com.fongmi.android.tv.bean.**`）。
- `Result` 是通用信封：包含 `List<Vod>`、`List<Class>`、`filters`、播放 `url`、`header`、`parse`、`drm`、`subs`、`danmaku`。
- `Vod` 双重注解：SimpleXML（type-0 站点）+ Gson。
- `Config.type`：0=点播，1=直播，2=壁纸。
- 历史记录键格式：`siteKey@@@vodId`。

---

## 数据库

Room 数据库名 `"tv"`，当前版本 **35**。30→35 的迁移定义在 `Migrations.java` 中，30 以下版本使用 `fallbackToDestructiveMigration`。

实体：`Keep`、`Site`、`Live`、`Track`、`Config`、`Device`、`History`。
历史记录 TTL：60 天。数据库备份：gzip `.bk.gz`，保留最近 7 份。

---

## 网络栈（`catvod` 模块）

所有 HTTP 请求经由 `catvod/.../OkHttp.java` 统一管理，基于 OkHttp 5.x。

- **DoH**：可配置 bootstrap IP。
- **Host DNS 覆盖**：支持通配符 `*`。
- **按 Host 正则的代理规则**：HTTP/HTTPS/SOCKS4/SOCKS5。
- **CORS 头注入**：按 Host 规则注入。
- **广告过滤**：通过配置 `ads` 字段的域名黑名单。
- **WebView 嗅探**：正则拦截媒体 URL，支持 UA 伪装。
- 自定义拦截器：`AuthInterceptor`、`RequestInterceptor`、`ResponseInterceptor`。

---

## 本地 HTTP API（Server）

`Server`（单例）在 9978–9998 端口中选取第一个可用端口启动 `Nano`（NanoHTTPD）。

接口路径：`/action`、`/cache`、`/media`、`/file`、`/parse`、`/proxy`、`/device`、`/tvbus`。
Web 遥控 UI 从 `app/src/main/assets/index.html` 提供服务。
完整接口文档见 `/docs/LOCAL.md`。

---

## 产品风味

| 风味 | 启动器 | 额外特性 |
|------|--------|---------|
| `leanback` | TV（`LEANBACK_LAUNCHER`） | 方向键导航、`BootReceiver` 开机自启、DLNA DMR（渲染端）、TV banner |
| `mobile` | 手机（`LAUNCHER`） | 扫码（`ScanActivity`）、生物识别、画中画、DLNA DMC（控制端） |

两种风味均支持 `arm64_v8a` 和 `armeabi_v7a`，共产出 4 个 APK 变体。

---

## 内置 AAR 库

位于 `app/libs/`：

| 文件 | 用途 |
|------|------|
| `tvbus-release.aar` | TVBus 直播流协议 |
| `thunder-release.aar` | 迅雷磁力链接 & ed2k 链接 |
| `jianpian-release.aar` | 尖片 `jianpian://` 协议 |
| `forcetech-release.aar` | ForceTech 流协议 |
| `dlna-core/dmc/dmr-release.aar` | UPnP/DLNA（基于 Cling） |
| `dfm-release.aar` | DanmakuFlameMaster（弹幕） |
| `hook-release.aar` | 包管理器身份伪装 |

---

## 配置加密格式

`Decoder` 支持三种配置格式：
1. **明文 JSON** — 直接使用。
2. **Base64** — 8 位字母数字前缀 + `**` + Base64 内容。
3. **AES-CBC 十六进制** — 字符串以 `2423` 开头，密钥从配置内容派生。

---

## 参考文档

权威参考文档位于 `/docs/`：
- `CONFIG.md` — 完整配置 JSON 字段说明（VodConfig、LiveConfig、Site、Parse、Live、Group、Channel 等）
- `SPIDER.md` — Spider API 规范（方法签名、返回 JSON 格式）
- `LOCAL.md` — 本地 HTTP API 接口说明
- `LIVE.md` — 直播源格式说明

---

## Agent 常见陷阱

1. **两套独立 UI 树。** `app/src/leanback/` 和 `app/src/mobile/` 不共享任何 Activity。修复 UI 问题时，先确认影响的是哪个风味，再编辑对应目录。

2. **Bean 字段名必须保持稳定。** Gson 和 SimpleXML 依赖字段名。重命名 Bean 字段会导致配置解析和数据库反序列化失败。

3. **ProGuard 保留 Bean 和爬虫。** 不要删除 `app/proguard-rules.pro` 中对 `bean.**`、`quickjs.method.**`、`catvod.crawler.**` 的 keep 规则。

4. **单例初始化顺序有严格要求。** `VodConfig` 必须在 `BaseLoader` 之前初始化；`BaseLoader` 从 `VodConfig.get().getSpider()` 获取 JAR 爬虫 URL。初始化顺序参考 `HomeActivity.initConfig()`。

5. **线程安全。** `App.post()` 用于主线程调度。后台操作更新 `LiveData` 时必须使用 `postValue()`，禁止使用 `setValue()`。

6. **解析类型不可互换。** `type=0` 的 `Parse` 需要运行中的 WebView（仅在 Activity 内有效）。类型 1–4 可在后台安全执行。

7. **Room 迁移必须是增量式的。** 禁止删除或重命名已有列；仅在可接受数据丢失时才使用 `fallbackToDestructiveMigration`。当前版本为 35。

8. **EventBus 注册。** 所有 Activity 必须在 `onCreate` 调用 `EventBus.getDefault().register(this)`，在 `onDestroy` 调用 `unregister(this)`（已在 `BaseActivity` 中完成）。Fragment 如需订阅事件，须手动注册和注销。

9. **Config 类型整数。** `Config.type` 值：`0`=点播，`1`=直播，`2`=壁纸。每种类型同一时间只能有一个活跃配置。

10. **本地服务器端口。** 禁止硬编码端口 9978；始终通过 `Server.get().getPort()` 获取实际绑定端口。
