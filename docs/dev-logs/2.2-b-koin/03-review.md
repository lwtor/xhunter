# 第 2.2.b 步 Review 报告

> 阶段 D 产物 · 创建于 2026-06-03 · 验收范围：Koin DI 接入 + 顺手清理 TODO-2.2-1
>
> **2026-06-03 15:18 更新**：用户决定 **撤销 BLOCKER-1**，`MainScreen.kt` 旧 `MainScreenPreview()` 保留现状，不再阻塞验收。本步 Review **通过**，进入阶段 E。TODO-2.2-1 改为遗留项登记到 `docs/ROADMAP.md` 与 `04-summary.md`。

---

## 一、改动总览

| 操作 | 文件 | 行数 |
| --- | --- | --- |
| 修改 | `gradle/libs.versions.toml` | +3 |
| 修改 | `shared/build.gradle.kts` | +1 |
| 修改 | `androidApp/build.gradle.kts` | +1 |
| 新建 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt` | +12 |
| 新建 | `androidApp/src/main/kotlin/com/lwtor/xhunter/XHunterApplication.kt` | +22 |
| 修改 | `androidApp/src/main/AndroidManifest.xml` | +1 |
| 修改 | `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt` | +6 / -3 |

总计 **5 修改 + 2 新建**（与 spec 一致）。lint 跨模块均 0 错误 0 警告。

---

## 二、Review 结论分栏

### ✅ 通过项（做得好的）

| 项 | 说明 |
| --- | --- |
| Koin 依赖配置 | `libs.versions.toml` 用 version catalog 统一管理 4.0.0；`shared` 接 `koin-core`，`androidApp` 接 `koin-android`，分层正确 |
| Module DSL 选型 | 用 `factory<RootComponent> { (ctx) -> ... }` 而非 `single`，符合 Activity 重建语义，避免 Context 失效 |
| 显式 `<RootComponent>` 接口注册 | 调用方 `getKoin().get { ... }` 拿到的是接口，调用方不需要知道 `DefaultRootComponent`，符合依赖倒置 |
| `XHunterApplication` 启动顺序 | `super.onCreate()` 先调，再 `startKoin {}`，未触发 Koin 初始化前置依赖问题 |
| `androidLogger(Level.INFO)` | Debug 阶段合理，方便 logcat 验证；上线前替换 Level 已在 spec 第 267 行登记为已知点 |
| Manifest `android:name=".XHunterApplication"` | 相对类名写法正确（基于 `package="com.lwtor.xhunter"`） |
| MainActivity 顺序修正 | `super.onCreate` → `defaultComponentContext()` → `getKoin().get` → `setContent`，已按 02-qa.md Q2 的修正落实 |
| 模块解耦 | Module 定义在 commonMain（未来 iOS 可复用），启动在 androidApp（平台粘合），KMP 分层标准做法 |
| Lint 干净 | androidApp + shared/commonMain 跨模块均 0 错误 |

### ⚠️ 建议项（不阻塞，但建议下一步顺手处理）

| 编号 | 项 | 文件 | 当前状态 | 建议 |
| --- | --- | --- | --- | --- |
| TODO-2.2-b-1 | **MainActivity 注释与实际代码矛盾** | `androidApp/.../MainActivity.kt` 第 15-16 行 | 注释还是旧版「**必须在 super.onCreate 之前调用**，它内部要接管 savedInstanceState」，但实际代码已经改为 `super.onCreate` 之后调用 | 删掉旧注释或改成：`// Decompose 3.x：defaultComponentContext() 内部访问 SavedStateRegistry，必须在 super.onCreate 之后调用` |
| TODO-2.2-b-2 | **类名大小写未对齐 spec** | `XHunterApplication.kt` 文件名 + 类名 | 实际命名是 `XHunterApplication`（大 H 双驼峰），spec 写的是 `XhunterApplication`（小 h） | 见下面"命名规范讨论"——**两种都合理**，本步不强制统一，由你决定后续基线（PascalCase 推荐 `XhunterApplication`，匹配产品名 `xhunter`） |
| TODO-2.2-b-3 | `XHunterApplication` 内部空行风格 | 第 11、14、21 行 | 类内部多了 2 处空行 | 个人风格偏好，不动 |

### ❌ 阻塞项（必须修复才能进入阶段 E）

> **2026-06-03 15:18 更新**：本节阻塞项已被用户主动撤销，全部降级为遗留项。原阻塞分析保留以备未来回看。

| 编号 | 项 | 文件 | 严重性 |
| --- | --- | --- | --- |
| ~~BLOCKER-1~~ → **LEGACY-2.2-b-1** | ~~TODO-2.2-1 未清理~~（用户决定保留） | `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` 第 60-73 行残留旧 `MainScreenPreview()` | 已撤销，登记为遗留项 |

**为什么是阻塞**：
- spec 第 12 行验收清单 ✅ 第 4 项「顺手清理 TODO-2.2-1：删除 MainScreen.kt 第 60-73 行残留的旧 MainScreenPreview()」
- spec 第 250 行「✅ TODO-2.2-1 清理：MainScreen.kt 不再有 MainScreenPreview() 残留代码」
- 这是本步 B-3.4 子任务，跟 Koin 接入是绑在一起验收的

**修复动作（用户操作）**：

1. 删 `MainScreen.kt` 第 60-73 行：

```kotlin
// 删除这一整段
@Preview
@Composable
private fun MainScreenPreview() {
    MainScreen(
        component = object : RootComponent {
            override val selectedTab: Value<MainTab>
                get() = MutableValue(MainTab.HOME)

            override fun onTabSelected(tab: MainTab) {

            }
        },
    )
}
```

2. 同步删除已无用的 import（顶部第 15、17、18 行）：

```kotlin
import androidx.compose.ui.tooling.preview.Preview        // 删
import com.arkivanov.decompose.value.MutableValue          // 删
import com.arkivanov.decompose.value.Value                 // 删
```

> ⚠️ `Value` 是否要删？
>
> 第 18 行 `import com.arkivanov.decompose.value.Value` 在 `MainScreen` 主体里**没用到**（`subscribeAsState` 是 extension function，不需要 Value 类型在 import 里），所以删掉它没问题。AS 的 "Optimize Imports" 也会自动建议删。

3. 保留：

```kotlin
import com.arkivanov.decompose.extensions.compose.subscribeAsState   // 保留
```

跨模块 Preview 现在统一在 `shared/src/androidMain/kotlin/com/lwtor/xhunter/ui/main/MainScreenPreview.kt`（2.2.a 步引入），删除 commonMain 的旧版本不会丢预览能力。

修完后 AS 重新构建一次，确认无报错；然后回到 Review，我把这一项打钩进入阶段 E。

---

## 三、命名规范讨论（建议项 TODO-2.2-b-2 展开）

你写的是 `XHunterApplication`（大 H），spec 写的是 `XhunterApplication`（小 h）。两种都常见，但项目内必须**选一个并贯彻到底**。

### 选项 A：`XhunterApplication`（小 h，推荐）

**理由**：
- 用户记忆里登记的产品名是 **xhunter**（全小写一个词），不是「X Hunter」
- Kotlin/Java PascalCase 规则：把"一个词"首字母大写 → `Xhunter`，而不是把每个字母组段都大写
- 与你 applicationId `com.lwtor.xhunter`、namespace `com.lwtor.xhunter`、产品名一致
- 类比：Google 的 `Gmail` 类名是 `GmailActivity`，不是 `GMailActivity`

**影响范围（如果改）**：
- 文件名：`XHunterApplication.kt` → `XhunterApplication.kt`
- 类名：`class XHunterApplication` → `class XhunterApplication`
- Manifest：`android:name=".XHunterApplication"` → `.XhunterApplication`
- 内部 `this@XHunterApplication` → `this@XhunterApplication`

### 选项 B：`XHunterApplication`（大 H，保留现状）

**理由**：
- 视觉上更明确「X + Hunter」两个词的分割
- 部分团队偏好把品牌缩写中的字母全大写（类似 `IOSDevice` vs `IosDevice` 的争论）

**风险**：
- 与产品名 `xhunter`（一个词）语义不一致
- 后续如果你又想新建 `XhunterTheme.kt`、`XhunterApp.kt` 等类，可能不自觉混用大小写

### 我的建议

**改成 `XhunterApplication`**（小 h），原因：你的产品名锚点已定为 `xhunter`（一个词全小写），Kotlin PascalCase 应保持词单元一致性。但**这是建议项，不阻塞验收**——你说继续保留大 H 我也不再叨叨，把决定记到 LEARNING_NOTES.md 即可。

如果决定改，下次顺手改即可（涉及 4 处：文件名 + 类名 + Manifest + 内部 `this@` 引用）。

---

## 四、阶段 D 收口

✅ **2026-06-03 15:18：Review 通过，进入阶段 E**

- 用户决定撤销 BLOCKER-1，原因：暂不清理 commonMain 旧 Preview，保留现状
- 建议项 TODO-2.2-b-1 / TODO-2.2-b-2 / TODO-2.2-b-3 + LEGACY-2.2-b-1 全部登记到 `04-summary.md` 与 `docs/ROADMAP.md` 遗留事项区
- 接下来：写 04-summary.md → 更新 docs/CHANGELOG.md → 更新 docs/ROADMAP.md

