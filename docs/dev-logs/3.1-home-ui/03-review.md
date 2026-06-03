# 第 3.1 步 Review 报告（阶段 D）

> 创建时间：2026-06-03 16:49
> 修订时间：2026-06-03 16:55（按新协作边界重写）
> Review 范围：`MainTab.kt` / `MainScreen.kt` / `HomeScreen.kt`
> 验收基线：仅逻辑层（编译、KMP 边界、状态流、模块依赖、平台 API 隔离）

---

## 协作边界声明（本次 Review 起生效）

**AI 只负责逻辑部分，不再介入 UI 细节**。

| 不再 Review 的范围 | 由谁决定 |
| --- | --- |
| 布局结构（用什么组件、几列、间距、padding、aspectRatio） | 用户 |
| 视觉细节（颜色、圆角、字号、图标选型） | 用户 |
| 文案措辞（"探索" vs "发现"、Tab 数量与标签） | 用户 |
| 文件组织（要不要拆 ComicCard 独立文件、放哪个目录） | 用户 |
| 命名风格（单复数、变量命名习惯） | 用户 |

**仍 Review 的范围**：编译错误、KMP commonMain 引平台 API、状态/数据流、生命周期/资源泄漏、模块依赖方向、expect/actual 接线、MVI/Decompose/Koin 用法。

---

## 总体结论

❌ **未通过**——存在 **1 个致命编译错误**（KMP 平台边界违规）。

修复后即可进阶段 E 归档。

| 类别 | 数量 |
| --- | --- |
| ❌ 阻塞项（必修） | **1** |
| ⚠️ 建议项 | **1** |
| ✅ 通过项 | 4 |

---

## ❌ 阻塞项（必修，不修无法进阶段 E）

### B1. `MainScreen.kt` 引用 JDK 内部 API，违反 KMP commonMain 平台隔离

**位置**：`MainScreen.kt:20`

```kotlin
import com.sun.tools.javac.Main
```

**问题**：

- `com.sun.tools.javac.*` 是 **JDK 内部 API**（Oracle 私有包），不属于 Java 标准库
- KMP `commonMain` 必须保持平台无关，**严禁引用 `com.sun.*` / `sun.*` / JVM-only 包**
- iOS 端（Kotlin/Native）和 Desktop 端（如启用其他 JVM）都没有这个包，编译直接失败
- 当前 Android 端能编过是因为 Android Gradle Plugin 把 `commonMain` 的 JVM 兼容部分透传了，但这不是合规用法
- 这行 import 在文件里**完全没被使用**（IDE 自动补全把你想打的 `Main` 误关联到 javac）

**修复**：直接删除第 20 行。

**为什么这是阻塞**：这是 KMP 项目第一条铁律——commonMain 不能见到任何 `com.sun.*`。本项目目标是 Android/iOS/Desktop 三端共享代码，今天放过去明天 iOS 集成时一定会爆炸。

---

## ⚠️ 建议项

### S1. `MainTab.kt` 残留未使用的 `import androidx.compose.material.icons.filled.Person`

**位置**：`MainTab.kt:7`

`PROFILE` 枚举值已删除，`Person` 图标没人用了。

**为什么提这条**：未使用 import 不是 UI 细节，属于**代码卫生 / 静态检查**范畴——detekt / ktlint / Android Lint 都会报，进 CI 会拦。

**修复**：删除该行 import（IDE 一键 Optimize Imports 即可）。

不强制，本步不修也能过。

---

## ✅ 通过项（逻辑层做对的）

- ✅ `MainTab` 枚举改名 `PROFILE → CATEGORIES`，编译期重构干净，无残留引用
- ✅ `MainScreen.kt` 中间区域 `when (selected) { HOME -> HomeScreen() ... }` 分发结构正确，符合本步 spec 的"按 selectedTab 分发子页面"约定
- ✅ Decompose `subscribeAsState()` + `RootComponent` 接线未被破坏，状态流仍然由 Component 驱动
- ✅ HomeScreen 内部状态用 `remember { mutableStateOf(...) }` 临时持有，符合 spec 第 0.3 节"3.1 步暂不引 ViewModel，3.2 步再迁移"的约定，没有过早优化

---

## 修复后的自检清单（仅逻辑维度）

修完后请确认：

1. ✅ `MainScreen.kt:20` 的 `import com.sun.tools.javac.Main` 已删除
2. ✅ Android 模拟器能正常编译并启动，App 不崩
3. ✅ 4 Tab 切换正常，状态由 Decompose Component 驱动（不是脱管的 mutableStateOf）
4. ✅ 主页 Tab 内部的二级 Tab 状态在切走再切回时丢失是符合预期的（3.1 步用 remember，3.2 步迁 ViewModel 后再讨论是否要保留）

UI 长什么样、Tab 命名、卡片样式、文件怎么拆——**不在 Review 范围内**，你自己定。

---

## 流程

修完 B1（删一行）后说：

- **`done`** / **`review`** → 我重新进阶段 D（这次只看逻辑，预计秒过）
- 直接说 **`pass`** / **`跳过 review`** → 我直接进阶段 E 归档（如果你已经自己删完确认过了，就走这条路省一轮）

S1 修不修都行，不影响通过。
