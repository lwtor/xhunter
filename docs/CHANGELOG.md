# CHANGELOG

每完成一个**大步骤**追加一段。格式：

```
## YYYY-MM-DD 第 X 步 <步骤名>

**改动文件清单**：
- `path/to/file1` — 简要说明
- `path/to/file2` — 简要说明

**功能变化**：
- 一句话描述用户能看到的变化

**学习要点**（可选）：
- 本步骤新接触的 KMP / Compose / 架构知识
```

---

## 2026-06-02 计划制定完成

**改动文件清单**：
- `docs/README.md` — 项目介绍 + 怎么跑起来
- `docs/DEVELOPMENT_RULES.md` — 编码 / Git / 文档规范
- `docs/ROADMAP.md` — 30+ 步骤总览（按"看得见的页面"切）
- `docs/CHANGELOG.md` — 本文件

**功能变化**：
- 项目工作区准备就绪，文档骨架完成

**学习要点**：
- KMP + CMP 技术栈整体蓝图
- MVI + Clean Architecture 在 KMP 项目下的分层方式
- 30+ 小步骤的拆解节奏：每个页面拆 UI / VM / 数据 三小步

---

## 2026-06-02 第 1 步 项目搭建（极简）

**改动文件清单**：
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt` — 在 Column 顶部新增 xhunter 标题 Text（headlineLarge）
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/GreetingUtil.kt` — `sayHello("Android")` 返回值改为 `"Hello xhunter on Android!"`
- `docs/README.md` / `docs/DEVELOPMENT_RULES.md` / `docs/ROADMAP.md` / `docs/CHANGELOG.md` — 第 1 步建立的 4 份文档

**工程基线（来自 kmp.jetbrains.com 向导生成）**：
- 工程目录：`/Users/lwtor/ai_workspace/xhunter/`
- `rootProject.name = "xhunter"`
- Android `applicationId = "com.lwtor.xhunter"` / `namespace = "com.lwtor.xhunter"`
- Kotlin 包名根：`com.lwtor.xhunter`
- 模块结构：`shared`（commonMain Compose UI）+ `androidApp`
- 关键版本：AGP 9.0.1 / Kotlin 2.3.21 / Compose Multiplatform 1.11.0 / compileSdk 36 / minSdk 24
- Gradle 守护进程 JDK：21（Amazon Corretto，由 `gradle/gradle-daemon-jvm.properties` 声明）
- 字节码目标：JVM 11

**功能变化**：
- Android 模拟器/真机启动后看到浅紫色背景 + 顶部 "xhunter" 标题
- 点击 "Click me!" 按钮显示 Compose Logo 与 "Compose: Hello xhunter on Android!"
- 验证了 commonMain Compose、`expect/actual` 平台名、Compose Resources 三个 KMP 关键能力

**学习要点**：
- KMP 新版向导默认生成 `gradle/gradle-daemon-jvm.properties`，AS 因此把 Gradle JDK 设置切换到 "Version + Vendor" 的 criteria 形式（与普通 Android 项目的下拉框 UI 不同）
- "运行 Gradle 守护进程的 JDK"（21）与"产出字节码目标"（11）是两件事，可以不一致
- `commonMain` 里的 Compose 函数能直接被 `androidApp` 复用，是 KMP UI 共享的最小验证

---

## 2026-06-03 第 2.1 步 主页框架 — 底部 4 Tab UI 骨架

**改动文件清单**：
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainTab.kt` — 新增，4 Tab 元数据 enum（Home/Favorites/Explore/Profile，含 label + Material `ImageVector` 图标）
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` — 新增，`Scaffold` + `NavigationBar` 主页框架，`rememberSaveable` 保存当前 Tab，私有 `MainBottomBar` 抽出底栏渲染
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt` — 改造：移除 KMP 向导默认 hello 模板，改为 `MaterialTheme { MainScreen() }`
- `gradle/libs.versions.toml` — 追加 `compose-material-icons-core` 依赖（`org.jetbrains.compose.material:material-icons-core:1.7.3`）
- `shared/build.gradle.kts` — `commonMain.dependencies` 追加 `implementation(libs.compose.material.icons.core)`
- `docs/dev-logs/2.1-bottom-tabs-ui/01-spec.md` / `02-qa.md` / `03-review.md` / `04-summary.md` — 本步开发文档归档

**功能变化**：
- App 启动后**不再**显示 KMP 向导的 "Click me" 默认页，改为底部 4 Tab 主页框架
- 4 Tab：首页 / 收藏 / 探索 / 个人，点击切换内容区文字（如"首页 - TODO"）
- 选中 Tab 在 Material3 默认配色下高亮，图标 + 文字双行常显
- 旋转屏幕（横竖屏切换）后，当前选中 Tab **不丢失**

**学习要点**：
- `Scaffold` 的 `bottomBar` 槽位 + content lambda 拿到的 `innerPadding` 必须应用到内容容器，否则被底栏遮住
- `rememberSaveable` vs `remember`：前者走 `Bundle` 序列化，配置变更/进程被回收后能恢复；Kotlin enum 在 JVM 上自动实现 `Serializable` 可直接保存，无需写 `Saver`
- `MainTab.entries` 是 Kotlin 1.9+ 的属性式 API，零开销取代旧 `values()`
- Compose Multiplatform 的 `material3` 依赖**不会自动传递**引入 Material Icons，`material-icons-core` 是独立 artifact 需单独声明（Q2 踩坑记录）
- 业务 Composable 不包 `MaterialTheme`，由入口 Composable（`App.kt`）统一提供

---

## 2026-06-03 第 2.2.a 步 主页框架 — Decompose 路由接入

**改动文件清单**：
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/RootComponent.kt` — 新增，`RootComponent` 接口 + `DefaultRootComponent` 实现，承载 `selectedTab: Value<MainTab>` 状态（类委托 `ComponentContext`）
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` — 改造：签名改为 `MainScreen(component: RootComponent)`，删 `rememberSaveable`，改用 `component.selectedTab.subscribeAsState()` 订阅状态
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt` — 签名改为 `App(rootComponent: RootComponent)`，去掉 `@Preview`
- `shared/src/androidMain/kotlin/com/lwtor/xhunter/ui/main/MainScreenPreview.kt` — 新增，androidMain 跨模块 Preview（4 Tab 各一份 + 私有 `PreviewRootComponent` 假实现）
- `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt` — `defaultComponentContext()` 在 `super.onCreate` **之前**调用创建 `DefaultRootComponent`，再 `setContent { App(rootComponent = root) }`
- `gradle/libs.versions.toml` — 新增 `decompose = "3.2.2"` 版本 + `decompose` / `decompose-extensions-compose` 两个 library 别名
- `shared/build.gradle.kts` — `commonMain.dependencies` 追加 decompose + decompose-extensions-compose
- `androidApp/build.gradle.kts` — 追加 `implementation(libs.decompose)`（让 `defaultComponentContext()` 可用）
- `docs/dev-logs/2.2-a-decompose/01-spec.md` / `02-qa.md` / `03-review.md` / `04-summary.md` — 本步开发文档归档

**功能变化**：
- 旋转屏幕（横竖屏切换）后，**当前选中 Tab 不丢失**（与 2.1 视觉一致，但底层换成 Decompose 驱动）
- App 启动入口从"自管理状态"切换为"由 Decompose RootComponent 驱动状态"，为后续详情页/阅读器嵌套导航打好基础

**学习要点**：
- Decompose 心智模型：Component = ViewModel + Navigation 合体；状态用 `Value<T>` / `MutableValue<T>` 表达，类似 `StateFlow` 范式（`MutableValue` 私有 + `Value` 对外只读）
- 类委托 `class X : Y by y`：`DefaultRootComponent` 通过 `ComponentContext by componentContext` 自动获得全部上下文方法，为后续 `childContext("xxx")` 留好通道
- `Value.subscribeAsState()`（来自 `decompose-extensions-compose`）是 Decompose ↔ Compose 的桥接点；Component 本身完全不依赖 Compose，**可以纯 JVM 单测**
- **关键坑点**：`defaultComponentContext()` 必须在 `super.onCreate(savedInstanceState)` **之前**调用（要 hook savedStateRegistry 注册窗口）—— spec 第一版写错，已在编码中纠正
- commonMain ↔ androidMain Preview 拆分：带必填参数的 Composable 不能直接 `@Preview`，改放 `shared/src/androidMain` 用私有假实现 + 4 Tab 各一份，IDE 能就近渲染（比放 androidApp 体验好）

**遗留项**：
- TODO-2.2-1：`MainScreen.kt` 第 60-73 行旧 `MainScreenPreview()` 残留，建议在 2.2.b 顺手清理（详见 `docs/dev-logs/2.2-a-decompose/04-summary.md` §五）
- TODO-2.2-2（可选）：`MainScreen.kt` 内层 Box 嵌套可扁平化为单层 `contentAlignment = Alignment.Center`

---

<!-- 第 2.2.b 步完成后从这里开始追加 -->

## 2026-06-03 第 2.2.b 步 主页框架 — Koin DI 接入

**改动文件清单**：
- `gradle/libs.versions.toml` — 新增 `koin = "4.0.0"` 版本 + `koin-core` / `koin-android` 两个 library 别名
- `shared/build.gradle.kts` — `commonMain.dependencies` 追加 `implementation(libs.koin.core)`
- `androidApp/build.gradle.kts` — 追加 `implementation(libs.koin.android)`
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt` — 新增，`sharedModule = module { factory<RootComponent> { (ctx: ComponentContext) -> DefaultRootComponent(componentContext = ctx) } }`
- `androidApp/src/main/kotlin/com/lwtor/xhunter/XHunterApplication.kt` — 新增，`Application.onCreate` 中 `startKoin { androidLogger(Level.INFO); androidContext(this@XHunterApplication); modules(sharedModule) }`
- `androidApp/src/main/AndroidManifest.xml` — `<application>` 增加 `android:name=".XHunterApplication"`
- `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt` — 改为 `super.onCreate` → `defaultComponentContext()` → `getKoin().get<RootComponent> { parametersOf(ctx) }` → `setContent` 顺序
- `docs/dev-logs/2.2-b-koin/01-spec.md` / `02-qa.md` / `03-review.md` / `04-summary.md` — 本步开发文档归档

**功能变化**：
- 启动 logcat 过滤 `Koin` 关键字可见 `[Koin] Started 1 module(s)` 日志
- Tab 切换 / 横竖屏旋转保留状态等行为与 2.2.a **完全一致**（本步是基础设施重构，用户视角无新功能）
- `MainActivity` 不再 `new DefaultRootComponent(...)`，所有 Component 由 Koin 接管创建

**学习要点**：
- Koin 4.x 心智模型：`module {}` = 配方清单；`single<X> {}` = 全局单例；`factory<X> {}` = 每次现造（适合 `RootComponent` 这种持有 Activity 重建后会失效的 `ComponentContext` 的对象）；`get<X> { parametersOf(arg) }` = 带运行时参数取菜
- KMP 项目 DI 分层：模块定义放 `shared/commonMain`（iOS/Desktop 可复用），平台 actual 注册放 `<platform>Main`，启动 Koin 放各 App 入口（Android 是 `Application.onCreate`）
- **Decompose 3.x 与旧版关键差异**：`defaultComponentContext()` **必须在 `super.onCreate(savedInstanceState)` 之后**调用（新版会访问 `SavedStateRegistry`，需要 owner 已进入 CREATED）；旧版（≤ 2.x）是「之前」—— spec 一开始写错，跑闪退后定位修正
- 接口注入（`factory<RootComponent>` 而非 `factory<DefaultRootComponent>`）符合依赖倒置，调用方不需要知道实现类
- Module 用 `factory` 而非 `single`：避免 Activity 重建后旧 `ComponentContext` 被缓存继续用导致状态错乱

**遗留项**：
- LEGACY-2.2-b-1：`MainScreen.kt` 第 60-73 行旧 `MainScreenPreview()` 残留（用户本步决定保留，等 androidMain Preview 用顺手了再删）
- TODO-2.2-b-1：`MainActivity.kt` 第 15-16 行注释还是旧版「必须在 super.onCreate 之前」，与实际代码顺序矛盾，下一步顺手改
- TODO-2.2-b-2：类名大小写未对齐 spec（实际 `XHunterApplication` 大 H / spec `XhunterApplication` 小 h），建议改成小 h 与产品名锚点 `xhunter` 一致

---
