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

<!-- 第 2.2 步完成后从这里开始追加 -->
