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

<!-- 第 2 步完成后从这里开始追加 -->
