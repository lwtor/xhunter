# xhunter

将开源 Flutter 漫画阅读器 [venera](https://github.com/venera-app/venera)（v1.6.3，已归档）一比一复刻到 **Kotlin Multiplatform + Compose Multiplatform** 技术栈，覆盖 Android / iOS / Windows / macOS 四端。

> ⚠️ 本项目以**学习 KMP 为核心目标**，不追求商业可用，不重新设计 UI/逻辑，按 venera 现有功能复制即可。

## 项目状态

| 平台 | 状态 |
| --- | --- |
| Android | 🚧 开发中（MVP 阶段，第 1 步完成） |
| iOS | ⏳ 待启动（第 9 大步） |
| macOS / Windows Desktop | ⏳ 待启动（第 10 大步） |

## 技术栈速览

- 语言：Kotlin 2.0+（K2 编译器，当前向导给到 2.3.21）
- UI：Compose Multiplatform 1.7+（当前向导给到 1.11.0）
- 架构：MVI + Clean Architecture（SOLID）
- 导航：Decompose 3
- DI：Koin 4
- 网络：Ktor 3
- 数据库：SQLDelight 2
- 图片：Coil 3
- JS 引擎（漫画源插件）：Android Zipline / iOS JavaScriptCore / Desktop GraalJS

> 完整技术栈与引入步骤见 [`ROADMAP.md`](./ROADMAP.md) 与 [`ARCHITECTURE.md`](./ARCHITECTURE.md)（架构文档将在第 2.3 步回填）。

## 命名约定

| 项 | 值 |
| --- | --- |
| 产品名（用户可见） | xhunter |
| 工程目录 | xhunter |
| Android applicationId | `com.lwtor.xhunter` |
| iOS bundleId（待 9.1 步） | `com.lwtor.xhunter` |
| Kotlin 包名根 | `com.lwtor.xhunter` |

> 产品名一律小写 `xhunter`，禁止再出现 venera-kmp / Venera 等旧称（致谢段落除外）。

## 快速开始

### 环境要求

- macOS（开发主机，iOS 端必需）
- Android Studio Ladybug (2024.2) 或更高，已安装 **Kotlin Multiplatform 插件**
- JDK 17+（守护进程使用 JDK 21，由 `gradle/gradle-daemon-jvm.properties` 自动管理）
- Xcode 15+（iOS 端必需，第 9 步开始用到）
- Android SDK 34+

### 跑起来

```bash
# clone 后用 Android Studio 打开项目根目录，等 Gradle Sync 完成
# Run Configuration 选 androidApp，选一个 Android 模拟器，点 ▶️ Run
```

预期看到一个浅紫色背景的页面，顶部居中显示 **xhunter** 标题；点击 "Click me!" 按钮会显示 Compose Logo 与 **"Compose: Hello xhunter on Android!"**。

## 文档导航

| 文档 | 内容 | 状态 |
| --- | --- | --- |
| [`README.md`](./README.md) | 项目介绍 + 怎么跑起来 | ✅ 第 1 步建立 |
| [`DEVELOPMENT_RULES.md`](./DEVELOPMENT_RULES.md) | 编码规范 / Git 规范 / 命名约定 | ✅ 第 1 步建立 |
| [`ROADMAP.md`](./ROADMAP.md) | 30+ 步骤总览（按"看得见的页面"切） | ✅ 第 1 步建立 |
| [`CHANGELOG.md`](./CHANGELOG.md) | 每完成大步追加 | ✅ 第 1 步建立 |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | 整体架构 + 模块依赖图 | ⏳ 第 2.3 步回填 |
| [`MODULES.md`](./MODULES.md) | 各模块职责说明 | ⏳ 第 2.3 步回填 |
| [`LEARNING_NOTES.md`](./LEARNING_NOTES.md) | KMP 学习笔记（用户自填） | ⏳ 边做边记 |

## 协作模式

本项目用户主导编码，AI 辅助。每一步遵循固定 5 环：

1. **AI 先发**：本步任务说明 + 骨架代码（带 TODO）+ 概念速查
2. **用户照做**：按文档新建文件、写实现、跑模拟器
3. **卡住找 AI**：贴报错/疑问，AI 给关键代码片段
4. **完成 AI Review**：用户提交后 AI 做 Code Review
5. **追加 CHANGELOG**：用户在 `CHANGELOG.md` 追加一行

复杂模块（阅读器手势、JS 引擎接入、平台 actual 实现）标注 **[AI 兜底]**，由 AI 写完整实现，用户负责理解 + 联调。

## 致谢

- [venera](https://github.com/venera-app/venera) — 原 Flutter 项目作者，本项目以其 v1.6.3 为复刻基线
- JetBrains — Kotlin Multiplatform & Compose Multiplatform
