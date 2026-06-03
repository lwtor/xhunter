# 第 2.2.a 步：接入 Decompose（路由 + 状态承载）

> 阶段 A 开发文档 / 任务方向 + 步骤拆解 + 验收标准
> 创建日期：2026-06-03

---

## 一、任务方向

把 2.1 的"4 Tab 切换"从 Compose 本地状态（`rememberSaveable`）升级为 **Decompose 路由 + Component 树**，为后续详情页/阅读器等多级导航打基础。

**核心目标**：**理解 Decompose 的"Component + ChildStack"模型**，让 4 Tab 的状态由 `RootComponent` 承载，UI 只读不写。

### 为什么要 Decompose？

当前 2.1 的方案：

```kotlin
// MainScreen.kt（2.1 版本）
var selected by rememberSaveable { mutableStateOf(MainTab.HOME) }
```

问题：
1. **状态绑死在 UI**：旋转屏幕靠 `rememberSaveable` 救命，复杂导航（详情页→阅读器→返回）会失控
2. **不可测**：状态逻辑混在 Composable 里，单元测试难写
3. **无法跨平台共享导航逻辑**：未来 iOS/Desktop 想共用导航栈，必须把状态从 Compose 抽离

Decompose 的解法：

```
Component 树（commonMain，纯 Kotlin，可测试）
    ↓ 持有 StateFlow / Value
UI 层（Composable，无状态，纯渲染）
```

**关键概念速查**（这一步只需理解前 3 个）：

| 概念 | 作用 | 类比 Android |
|---|---|---|
| `ComponentContext` | 给 Component 提供生命周期/状态保存能力 | Activity Context（但跨平台） |
| `Component` | 承载一屏（或一段）的状态与逻辑 | ViewModel + 导航逻辑 |
| `Value<T>` | Decompose 自己的响应式状态容器（类 StateFlow） | LiveData / StateFlow |
| `ChildStack` | 后入先出的导航栈（详情→阅读器场景） | FragmentManager backstack |
| `ChildSlot` | 单槽位（弹窗/抽屉场景） | DialogFragment |
| `ChildPages` | 横滑页（Pager） | ViewPager |

> 本步只用到 `ComponentContext` + `Component` + `Value`。`ChildStack` 留到第 4 步详情页跳转时再上。

---

## 二、技术决策已锁定

根据开工前 3 个决策点：

| 决策 | 选择 | 落地动作 |
|---|---|---|
| Q1 拆分粒度 | B 三小步 | 本步只做 Decompose，不动 Koin/MVI 基类 |
| Q2 模块拆分 | A 拆 core-common | **本步暂不拆**，2.2.c 才拆。Decompose 类先放 `shared/commonMain/com/lwtor/xhunter/ui/main/` |
| Q3 ViewModel 选型 | A Component 直接承载状态 | 不引入 androidx.lifecycle.ViewModel，状态用 `MutableValue<T>` |

**版本基准**：Decompose **3.2.2**（2025 主流稳定版）+ `decompose-extensions-compose` 同版本

---

## 三、步骤拆解

### Step 1：加依赖（5 分钟）

#### 1.1 编辑 `gradle/libs.versions.toml`

在 `[versions]` 区追加：
```toml
decompose = "3.2.2"
```

在 `[libraries]` 区追加：
```toml
decompose = { module = "com.arkivanov.decompose:decompose", version.ref = "decompose" }
decompose-compose = { module = "com.arkivanov.decompose:extensions-compose", version.ref = "decompose" }
```

#### 1.2 编辑 `shared/build.gradle.kts`

在 `commonMain.dependencies { ... }` 里追加：
```kotlin
implementation(libs.decompose)
implementation(libs.decompose.compose)
```

#### 1.3 Sync Gradle

AS 顶部出现 `Sync Now` 黄条 → 点一下 → 等同步完成。

**预期**：External Libraries 树下能看到 `decompose-3.2.2` 与 `extensions-compose-3.2.2`。

---

### Step 2：建 RootComponent（25 分钟，重点）

新建文件 `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/RootComponent.kt`：

```kotlin
package com.lwtor.xhunter.ui.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

/**
 * 应用根 Component。
 * 当前职责：承载底部 4 Tab 的选中态。
 *
 * 后续扩展（先了解，本步不做）：
 *   - 加 ChildStack：4 Tab 各自独立的导航栈（首页 → 详情 → 阅读器）
 *   - 加 deepLink：从外部链接打开指定漫画
 */
interface RootComponent {
    val selectedTab: Value<MainTab>

    fun onTabSelected(tab: MainTab)
}

class DefaultRootComponent(
    componentContext: ComponentContext, // 注入而不是创建——Decompose 的核心约定
) : RootComponent, ComponentContext by componentContext {

    private val _selectedTab = MutableValue(MainTab.HOME)
    override val selectedTab: Value<MainTab> = _selectedTab

    override fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }
}
```

**关键点提示**：
- **`ComponentContext by componentContext`**：用 Kotlin 类委托，让 `DefaultRootComponent` 自身就是一个 `ComponentContext`（这样未来要建子 Component 时，直接 `childContext("xxx")` 就能拿到子 context）
- **`MutableValue` vs `Value`**：内部用可变的，对外暴露只读的——和 `MutableStateFlow` / `StateFlow` 套路完全一致
- **构造函数注入 `componentContext`**：这是 Decompose 的"宪法"，**永远不要在 Component 内部 new ComponentContext**

---

### Step 3：改造 MainScreen（15 分钟）

把 `MainScreen.kt` 从"自己管状态"改为"消费 Component 状态"。

修改 `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt`：

```kotlin
package com.lwtor.xhunter.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@Composable
fun MainScreen(component: RootComponent) {
    val selected by component.selectedTab.subscribeAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { component.onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("当前 Tab：${selected.label}")
        }
    }
}
```

**变化点对比表**：

| 旧（2.1） | 新（2.2.a） | 说明 |
|---|---|---|
| `var selected by rememberSaveable { ... }` | `val selected by component.selectedTab.subscribeAsState()` | 状态来源外移 |
| `onClick = { selected = tab }` | `onClick = { component.onTabSelected(tab) }` | 改变状态走 Component 接口 |
| `MainScreen()` 无参 | `MainScreen(component: RootComponent)` | UI 变成纯函数 |

**`subscribeAsState()` 是什么？**
来自 `decompose-extensions-compose`，把 Decompose 的 `Value<T>` 转成 Compose 的 `State<T>`，自动订阅/反订阅。

---

### Step 4：在 App.kt 创建 RootComponent（10 分钟）

修改 `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt`：

```kotlin
package com.lwtor.xhunter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.lwtor.xhunter.ui.main.MainScreen
import com.lwtor.xhunter.ui.main.RootComponent

@Composable
fun App(rootComponent: RootComponent) {
    MaterialTheme {
        MainScreen(rootComponent)
    }
}
```

> 注意：`App` 不再自己 new RootComponent，而是**由调用方传入**。原因：RootComponent 的生命周期要绑定到 Activity，不能绑到 Composable。

---

### Step 5：在 Android 入口创建 RootComponent（15 分钟，平台相关）

修改 `androidApp/src/main/kotlin/com/lwtor/xhunter/android/MainActivity.kt`（具体路径以你工程为准）：

```kotlin
package com.lwtor.xhunter.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.lwtor.xhunter.App
import com.lwtor.xhunter.ui.main.DefaultRootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在 Activity.onCreate 里创建一次，绑定到 Activity 生命周期
        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
        )

        setContent {
            App(root)
        }
    }
}
```

**`defaultComponentContext()` 是什么？**
Decompose 提供的 Android 扩展函数，自动接入 `Activity.savedStateRegistry` 和 `lifecycle`。这一行让 Decompose 的状态保存（旋转屏幕、低内存恢复）"白嫖"Android 平台能力。

> 如果你的 MainActivity 不是 `ComponentActivity` 而是 `AppCompatActivity`，也 OK，`defaultComponentContext()` 同样可用。

---

## 四、验收标准

跑 `androidApp` 后：

| 编号 | 验收项 | 验证方式 |
|---|---|---|
| AC-1 | 4 Tab 切换正常 | 点击 4 个 Tab，中间文案 `当前 Tab：xxx` 跟随变化 |
| AC-2 | 旋转屏幕保留选中态 | 选中"探索"，模拟器 `Ctrl+F11` 旋转横屏，仍是"探索" |
| AC-3 | Lint 0 报错 | AS 底部 Build 输出无 ERROR/WARNING |
| AC-4 | App.kt 不再持有 selected 状态 | 全文件搜索 `rememberSaveable` 应只剩 0 处 |
| AC-5 | RootComponent 单测可写（不强制写） | 概念上验证 RootComponent 不依赖任何 Compose API，纯 Kotlin |

---

## 五、可能踩的坑（先看，少走弯路）

### Pit 1：依赖冲突

**症状**：sync 后报 `Duplicate class com.arkivanov.essenty.xxx`。
**原因**：Essenty 是 Decompose 的底层库，多版本冲突。
**解法**：本步只引入 decompose + extensions-compose，不要单独引 essenty。

### Pit 2：`subscribeAsState` 找不到

**症状**：IDE 红线 `Unresolved reference: subscribeAsState`。
**原因**：忘了引 `decompose-compose`（即 `extensions-compose`）。
**解法**：检查 libs.versions.toml 是否有 `decompose-compose` 别名，build.gradle.kts 是否 `implementation(libs.decompose.compose)`。

### Pit 3：旋转后 Tab 重置

**症状**：旋转屏幕后回到首页 Tab。
**原因**：MainActivity 没声明 `configChanges` 也没用 `defaultComponentContext()`，或者用了但 RootComponent 创建在 Composable 里（错位）。
**解法**：确认 `DefaultRootComponent(...)` 在 `onCreate` 里创建，不是在 `setContent { }` 里。

### Pit 4：`MainTab` 找不到

**症状**：`Unresolved reference: MainTab` in MainScreen.kt
**原因**：可能你 import 漏了。
**解法**：`MainTab` 是 2.1 已建好的 enum，路径 `com.lwtor.xhunter.ui.main.MainTab`，AS 应能 alt+enter 自动 import。

### Pit 5：`ComponentContext by componentContext` 看不懂

这是 Kotlin **类委托**语法（不是属性委托）。意思：
> "DefaultRootComponent 实现 ComponentContext 接口的所有方法，全部转发给构造函数传入的 componentContext 参数"

效果：`DefaultRootComponent` 既是 `RootComponent` 又是 `ComponentContext`，未来你 `someChild = ChildComponent(this.childContext("child"))` 就能用了。

---

## 六、本步不做的事（明确边界）

- ❌ Koin DI（2.2.b）
- ❌ MVI 基类（2.2.c）
- ❌ ChildStack 多级导航（第 4 步详情页才需要）
- ❌ 拆 core-common 子模块（2.2.c）
- ❌ 写单元测试（第 11.3 步统一补）

---

## 七、参考文档

- Decompose 官方教程（推荐）：https://arkivanov.github.io/Decompose/getting-started/quick-start/
- Compose 集成：https://arkivanov.github.io/Decompose/extensions/compose/
- 状态保存机制：https://arkivanov.github.io/Decompose/component/state-preservation/

> 不强制读完。文档里的"Counter Sample"代码读一遍，就理解 90% 了。

---

## 八、预估工作量

| 阶段 | 时间 | 内容 |
|---|---|---|
| 读文档 + 理解概念 | 20-30 min | 看 Decompose 快速开始 + 本 spec |
| 写代码（Step 1-5） | 60-70 min | 按步骤敲 |
| 跑通 + 调试 | 10-20 min | 处理 sync/编译错误 |
| **合计** | **1.5-2 小时** | |

> 如果踩坑卡住，**别硬磕超过 30 分钟**——直接贴报错给我，进入阶段 C 答疑。

---

**完成标识**：当 5 个验收项全部 ✅，跟我说 "**done**" 或 "**写完了**"，进入阶段 D Review。
