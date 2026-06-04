# 第 3.2 步 — 首页 ViewModel + MVI（教学级 Spec）

> 阶段 A 产出（重写版）。本 spec 面向 KMP 初学者，每个子步骤都给出**完整可运行的代码**和**为什么要这么写的解释**。你按顺序从 a 做到 g 即可，每做完一步可以先跑一下确认编译通过。

---

## 一、这步在干什么？（先建立全局观）

### 1.1 现状（3.1 步做完后）

```
HomeScreen.kt
├── private val placeholderComic = List(8) { ... }   ← 写死数据，直接在 Composable 文件里
├── @Composable fun HomeScreen()                      ← 无参数，自己管状态
└── 内部用 Column 渲染列表
```

问题：
- 状态和数据都"住在" UI 文件里，换 Tab 时数据不会变（都是同一份 `placeholderComic`）
- 没有"用户操作 → 状态改变 → UI 刷新"这条清晰的数据链路

### 1.2 目标（3.2 步做完后）

```
HomeScreen.kt
├── @Composable fun HomeScreen(component: HomeComponent)  ← 接收 Component，不再自己管状态
├── val state by component.state.subscribeAsState()       ← 从 Component 读状态
└── 根据 state.selectedSubTab / state.comics 渲染

HomeComponent.kt  ← 新文件：状态权威，所有逻辑在这里
├── interface HomeComponent { state; onIntent() }
└── class DefaultHomeComponent : HomeComponent
    ├── MutableValue<HomeState>     ← 持有可变状态
    ├── onIntent(SelectSubTab)      ← 收到意图后更新状态
    └── 写死的数据生成（按 Tab 返回不同列表）

HomeContract.kt  ← 新文件：MVI 契约（State/Intent/Effect 的定义）
├── HomeState    ← "页面长什么样"的数据描述
├── HomeIntent   ← "用户想干什么"的操作描述
├── HomeEffect   ← "一次性事件"（本步留空）
├── HomeSubTab   ← 二级 Tab 枚举
└── HomeComic    ← 单条漫画的数据模型
```

### 1.3 数据流（MVI 单向数据流）

```
用户点击"分类" Tab
    │
    ▼
HomeScreen 调用 component.onIntent(HomeIntent.SelectSubTab(CATEGORY))
    │
    ▼
DefaultHomeComponent.onIntent() 处理：
    1. 更新 _state.value = HomeState(selectedSubTab = CATEGORY, comics = 分类列表)
    │
    ▼
HomeScreen 里 val state by component.state.subscribeAsState() 自动感知变化
    │
    ▼
Compose 重组 → UI 显示分类数据
```

**关键理解**：UI 只负责"读 state + 派发 intent"，不负责改数据。数据怎么改，全在 Component 里决定。这就是"单向数据流"——数据只往一个方向走，好调试、好测试。

---

## 二、本步要新建/修改的文件总览

| # | 文件 | 操作 | 说明 |
| --- | --- | --- | --- |
| a | `shared/.../ui/home/HomeContract.kt` | 🆕 新建 | MVI 契约定义 |
| b | `shared/.../ui/home/HomeComponent.kt` | 🆕 新建 | Component 接口 + 默认实现 |
| c | `shared/.../di/SharedModule.kt` | ✏️ 修改 | 注册 HomeComponent |
| d | `shared/.../ui/main/RootComponent.kt` | ✏️ 修改 | 创建 HomeComponent 实例并暴露 |
| e | `shared/.../ui/main/MainScreen.kt` | ✏️ 修改 | 透传 homeComponent 给 HomeScreen |
| f | `shared/.../ui/home/HomeScreen.kt` | ✏️ 修改 | 改签名，从 Component 读状态 |
| g | `shared/.../ui/main/MainScreenPreview.kt` | ✏️ 修改 | Preview 桩适配新签名 |

> 下面的路径中 `shared/.../` 是简写，完整路径前缀是：
> `shared/src/commonMain/kotlin/com/lwtor/xhunter/`
> Android Preview 文件在：
> `shared/src/androidMain/kotlin/com/lwtor/xhunter/`

---

## 三、逐个子步骤（按顺序做）

### 子步骤 a：新建 HomeContract.kt（MVI 契约）

**做什么**：定义首页要用到的所有"数据模型"。就像盖房子先画图纸，这里定义的是"首页有哪些数据、用户能做什么操作"。

**文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeContract.kt`

**完整代码**：

```kotlin
package com.lwtor.xhunter.ui.home

/**
 * MVI 契约文件 —— 把 State / Intent / Effect / 辅助类型放在一个文件里。
 *
 * 为什么要分 State / Intent / Effect？
 * - State：描述"页面当前长什么样"，UI 只读它
 * - Intent：描述"用户想干什么"，UI 只发它
 * - Effect：描述"一次性事件"（比如弹 Toast、跳转页面），本步暂时用不到
 *
 * 这样拆开后，数据流是单向的：Intent → Component → State → UI
 * 调试时只需要看"收到了什么 Intent → 产生了什么 State"，逻辑清晰
 */

// ============ State ============

/**
 * 首页的完整状态。
 *
 * data class 的好处：
 * 1. 自动生成 equals/hashCode/copy/toString
 * 2. Compose 可以通过对比新旧 State 的 equals 来决定要不要重组
 * 3. 用 copy() 可以方便地只改一个字段：state.copy(selectedSubTab = CATEGORY)
 */
data class HomeState(
    val selectedSubTab: HomeSubTab = HomeSubTab.RECOMMEND,
    val comics: List<HomeComic> = emptyList(),
)

/**
 * 二级 Tab 枚举。
 * enum class 意味着只有这 3 个值，不可能出现别的。
 */
enum class HomeSubTab {
    RECOMMEND,   // 推荐
    CATEGORY,    // 分类
    RANKING,     // 排行
}

/**
 * 单条漫画的数据模型（本步只存文字信息，3.3 步会加封面 URL）。
 */
data class HomeComic(
    val id: String,
    val title: String,
    val author: String,
)

// ============ Intent ============

/**
 * 用户意图。
 *
 * sealed interface 的好处：
 * 1. 编译器强制你处理所有子类型（when 表达式不会漏）
 * 2. 不可能凭空构造一个"不属于任何已知类型"的 Intent
 * 3. 新增意图时加一个 data class 就行
 */
sealed interface HomeIntent {
    /** 用户点击了某个二级 Tab */
    data class SelectSubTab(val tab: HomeSubTab) : HomeIntent
    // 后续 3.3 步会加 LoadInitial / Retry 等
}

// ============ Effect ============

/**
 * 一次性事件（本步留空）。
 *
 * 什么时候需要 Effect？
 * - 弹 Toast / Snackbar
 * - 导航跳转到详情页
 * - 这些是"发生一次就完"的事件，不适合放在 State 里
 *   （否则旋转屏幕会重新弹一次 Toast）
 *
 * 等后续步骤需要时再往里面加子类型。
 */
sealed interface HomeEffect
```

**做完后验证**：直接编译，应该无报错。

---

### 子步骤 b：新建 HomeComponent.kt（状态权威）

**做什么**：创建首页的 Decompose Component。它是首页的"大脑"——持有状态、处理意图、更新状态。

**文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeComponent.kt`

**完整代码**：

```kotlin
package com.lwtor.xhunter.ui.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update

/**
 * 首页 Component 接口。
 *
 * 为什么要有接口 + 实现两份？
 * - 接口：给 UI 和 Preview 用的，只暴露"读状态"和"发意图"
 * - 实现：真正的逻辑代码，UI 不需要知道内部细节
 * - Preview 时可以传一个假实现（桩），不需要真的创建 Component
 *
 * 这跟第 2 步的 RootComponent 是一样的模式：
 *   interface RootComponent { val selectedTab; fun onTabSelected() }
 *   class DefaultRootComponent(...) : RootComponent
 */
interface HomeComponent {
    /** 首页当前状态（只读），UI 通过 subscribeAsState() 订阅 */
    val state: Value<HomeState>

    /** UI 调用此方法派发用户意图 */
    fun onIntent(intent: HomeIntent)
}

/**
 * 首页 Component 默认实现。
 *
 * 关键点解析：
 * 1. `ComponentContext by componentContext` —— 委托模式，让这个类拥有 Decompose 的生命周期能力
 *    （状态保存、返回栈等）。跟 DefaultRootComponent 的写法一样。
 *
 * 2. `MutableValue` vs `Value` —— 类似 StateFlow/MutableStateFlow 的关系：
 *    - MutableValue 可以写（_state.value = ... 或 _state.update { ... }）
 *    - Value 只能读（暴露给外部）
 *
 * 3. `update { }` —— 安全地更新状态，类似于 stateFlow.update { it.copy(...) }
 *    它保证在更新过程中拿到的是最新值，避免并发问题。
 *
 * 4. 写死数据 —— 3.3 步会替换成从 Repository 拉取，本步先硬编码。
 */
class DefaultHomeComponent(
    componentContext: ComponentContext,
) : HomeComponent, ComponentContext by componentContext {

    // 可变状态，内部使用；初始状态是"推荐 Tab + 推荐列表"
    private val _state = MutableValue(
        HomeState(
            selectedSubTab = HomeSubTab.RECOMMEND,
            comics = generateComics(HomeSubTab.RECOMMEND),
        )
    )

    // 只读状态，暴露给外部（UI 订阅这个）
    override val state: Value<HomeState> = _state

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectSubTab -> {
                // 用户切 Tab → 更新 selectedSubTab + 生成对应列表
                _state.update {
                    it.copy(
                        selectedSubTab = intent.tab,
                        comics = generateComics(intent.tab),
                    )
                }
            }
            // 后续 3.3 步加 LoadInitial 等时在这里加分支
        }
    }

    companion object {
        /**
         * 根据 Tab 生成写死的漫画列表。
         *
         * 不同 Tab 返回不同前缀，这样你在手机上切 Tab 时
         * 能肉眼看到数据变了（推荐 #1 vs 分类 #1）。
         *
         * companion object 里放，是因为它不依赖实例状态，
         * 跟"当前选了哪个 Tab"无关——你传什么 Tab 进来，它就生成什么列表。
         */
        private fun generateComics(tab: HomeSubTab): List<HomeComic> {
            val prefix = when (tab) {
                HomeSubTab.RECOMMEND -> "推荐"
                HomeSubTab.CATEGORY -> "分类"
                HomeSubTab.RANKING -> "排行"
            }
            return List(8) { i ->
                HomeComic(
                    id = "${tab.name.lowercase()}-$i",
                    title = "$prefix #${i + 1}",
                    author = "${prefix}作者 ${'A' + i}",
                )
            }
        }
    }
}
```

**做完后验证**：编译一下，确认无报错。

**理解检查点**：
- `MutableValue` 和 `Value` 的关系，类比 `MutableStateFlow` 和 `StateFlow`
- `update { it.copy(...) }` 是原子更新，不会丢失中间状态
- `ComponentContext by componentContext` 让 Component 能参与 Decompose 的生命周期管理

---

### 子步骤 c：修改 SharedModule.kt（注册 HomeComponent）

**做什么**：让 Koin 知道怎么创建 HomeComponent。这样其他地方就能通过 Koin 注入获取它。

**文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt`

**当前代码**：
```kotlin
package com.lwtor.xhunter.di

import com.arkivanov.decompose.ComponentContext
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.dsl.module

val sharedModule = module {
    factory<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(componentContext)
    }
}
```

**改成**：
```kotlin
package com.lwtor.xhunter.di

import com.arkivanov.decompose.ComponentContext
import com.lwtor.xhunter.ui.home.DefaultHomeComponent
import com.lwtor.xhunter.ui.home.HomeComponent
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.dsl.module

val sharedModule = module {
    factory<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(componentContext)
    }

    // HomeComponent —— 注意这里用 factory 不是 single
    // 原因：每个 ComponentContext 不同（如果将来有多实例），factory 每次拿参数创建新实例
    factory<HomeComponent> { (componentContext: ComponentContext) ->
        DefaultHomeComponent(componentContext)
    }
}
```

**解释**：
- `factory` vs `single`：`factory` 每次注入都新建，`single` 全局只建一个。Component 需要不同的 `ComponentContext`，所以用 `factory`
- `(componentContext: ComponentContext)` —— 这是 Koin 的**参数注入**，调用方在 `get()` 时传入：`get<HomeComponent> { parametersOf(childContext) }`

---

### 子步骤 d：修改 RootComponent.kt（创建并暴露 HomeComponent）

**做什么**：让 RootComponent 持有 HomeComponent 实例，这样 MainScreen 能拿到它并传给 HomeScreen。

**当前代码**：
```kotlin
package com.lwtor.xhunter.ui.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val selectedTab: Value<MainTab>
    fun onTabSelected(tab: MainTab)
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext {
    private val _selectedTab = MutableValue(MainTab.HOME)
    override val selectedTab: Value<MainTab> = _selectedTab

    override fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }
}
```

**改成**：
```kotlin
package com.lwtor.xhunter.ui.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.lwtor.xhunter.ui.home.HomeComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

interface RootComponent {
    val selectedTab: Value<MainTab>
    val homeComponent: HomeComponent     // ← 新增：暴露首页 Component
    fun onTabSelected(tab: MainTab)
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val _selectedTab = MutableValue(MainTab.HOME)
    override val selectedTab: Value<MainTab> = _selectedTab

    // ← 新增：创建 HomeComponent
    // childContext("home") 给 HomeComponent 分配一个独立的子上下文
    // 这样 HomeComponent 的状态保存、生命周期就跟着 RootComponent 走
    override val homeComponent: HomeComponent = get {
        parametersOf(childContext("home"))
    }

    override fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }
}
```

**关键解释**：

1. **`KoinComponent`** —— 让 `DefaultRootComponent` 具备从 Koin 容器里 `get()` 依赖的能力。必须加这个接口，否则 `get { parametersOf(...) }` 会报编译错误。

2. **`childContext("home")`** —— Decompose 的概念：
   - `childContext("home")` 创建一个"子上下文"，key 是 `"home"`
   - 子上下文的生命跟随父上下文（RootComponent）
   - 如果 App 被系统回收后恢复，childContext 能帮 HomeComponent 恢复状态
   - `"home"` 是一个唯一标识，确保每次拿到的是同一个子上下文

3. **`get { parametersOf(childContext("home")) }`** —— 从 Koin 容器获取 HomeComponent，把 childContext 作为构造参数传入。对应 SharedModule 里定义的 `factory<HomeComponent> { (componentContext: ComponentContext) -> ... }`

---

### 子步骤 e：修改 MainScreen.kt（透传 homeComponent）

**做什么**：让 MainScreen 把 RootComponent 里的 homeComponent 传给 HomeScreen。

**当前代码**中 HOME 分支是：
```kotlin
MainTab.HOME -> HomeScreen()   // 无参数
```

**改成**：
```kotlin
MainTab.HOME -> HomeScreen(component = component.homeComponent)
```

**完整修改后的 MainScreen.kt**：
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.lwtor.xhunter.ui.home.HomeScreen

@Composable
fun MainScreen(
    component: RootComponent,
) {
    val selected by component.selectedTab.subscribeAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = {
                            component.onTabSelected(tab)
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(text = tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (selected) {
                // ↓↓↓ 唯一改动：HomeScreen 现在接收 component 参数 ↓↓↓
                MainTab.HOME -> HomeScreen(component = component.homeComponent)
                MainTab.FAVORITES,
                MainTab.EXPLORE,
                MainTab.CATEGORIES -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = selected.label
                        )
                    }
                }
            }
        }
    }
}
```

> 注意：原来文件底部的 `@Preview` 函数 `MainScreenPreview` 已经移到了 `MainScreenPreview.kt` 里（子步骤 g 处理），所以 MainScreen.kt 里**不需要再保留** Preview 代码。

---

### 子步骤 f：修改 HomeScreen.kt（从 Component 读状态）

**做什么**：把 HomeScreen 改成"接收 Component、读 state、发 intent"的模式，删除文件内的写死数据。

**完整替换后的 HomeScreen.kt**：

```kotlin
package com.lwtor.xhunter.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState

/**
 * 首页 Composable。
 *
 * 改动要点（对比 3.1 步）：
 * 1. 签名从 HomeScreen() 改为 HomeScreen(component: HomeComponent)
 * 2. 不再自己持有 mutableStateOf / placeholderComic
 * 3. 所有状态从 component.state 读取
 * 4. 用户操作通过 component.onIntent() 派发
 *
 * UI 布局由你定，这里给一个最简可运行的版本：
 * - 顶部一行 FilterChip 作为二级 Tab
 * - 下方显示当前 Tab 的漫画列表（纯文字）
 */
@Composable
fun HomeScreen(
    component: HomeComponent,
) {
    // subscribeAsState()：把 Decompose 的 Value<HomeState> 转成 Compose 的 State<HomeState>
    // 这样当 Component 里的 state 变了，这里会自动触发重组
    val state by component.state.subscribeAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // === 二级 Tab 行 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            HomeSubTab.entries.forEach { tab ->
                FilterChip(
                    selected = tab == state.selectedSubTab,
                    onClick = {
                        // 用户点 Tab → 派发 Intent → Component 处理 → State 更新 → UI 重组
                        component.onIntent(HomeIntent.SelectSubTab(tab))
                    },
                    label = { Text(text = tab.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        // === 漫画列表（纯文字，3.3 步换卡片+图片） ===
        state.comics.forEach { comic ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                text = "${comic.title} - ${comic.author}"
            )
        }
    }
}

/**
 * 给 HomeSubTab 一个中文显示名。
 * 扩展属性，在 Composable 里用 tab.label 即可。
 */
private val HomeSubTab.label: String
    get() = when (this) {
        HomeSubTab.RECOMMEND -> "推荐"
        HomeSubTab.CATEGORY -> "分类"
        HomeSubTab.RANKING -> "排行"
    }
```

**理解检查点**：
- `val state by component.state.subscribeAsState()` —— `by` 是属性委托，每次 state 变化自动触发重组
- `component.onIntent(HomeIntent.SelectSubTab(tab))` —— 不直接改数据，而是"告诉 Component 我想干啥"
- 文件里不再有 `placeholderComic` 或 `mutableStateOf`——状态和数据全部由 Component 管

---

### 子步骤 g：修改 MainScreenPreview.kt（Preview 桩适配）

**做什么**：Preview 里的 `PreviewRootComponent` 需要实现新加的 `homeComponent` 属性。

**文件**：`shared/src/androidMain/kotlin/com/lwtor/xhunter/ui/main/MainScreenPreview.kt`

**完整替换**：

```kotlin
package com.lwtor.xhunter.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.lwtor.xhunter.ui.home.HomeComponent
import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeIntent
import com.lwtor.xhunter.ui.home.HomeState
import com.lwtor.xhunter.ui.home.HomeSubTab

/**
 * Preview 专用的 Fake RootComponent。
 *
 * 改动：新增 homeComponent 属性，返回一个假数据。
 * Preview 不需要真实的 Component 逻辑，只需要能让 @Preview 编译和渲染。
 */
private class PreviewRootComponent(
    initialTab: MainTab = MainTab.HOME,
) : RootComponent {
    override val selectedTab: Value<MainTab> = MutableValue(initialTab)
    override fun onTabSelected(tab: MainTab) = Unit

    // 假 HomeComponent，给 Preview 用
    override val homeComponent: HomeComponent = object : HomeComponent {
        override val state = MutableValue(
            HomeState(
                selectedSubTab = HomeSubTab.RECOMMEND,
                comics = List(3) {
                    HomeComic("preview-$it", "预览漫画 $it", "预览作者")
                }
            )
        )
        override fun onIntent(intent: HomeIntent) = Unit
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Home Tab")
@Composable
private fun MainScreenPreview_Home() {
    MainScreen(component = PreviewRootComponent(MainTab.HOME))
}

@Preview(showBackground = true, showSystemUi = true, name = "Favorites Tab")
@Composable
private fun MainScreenPreview_Favorites() {
    MainScreen(component = PreviewRootComponent(MainTab.FAVORITES))
}

@Preview(showBackground = true, showSystemUi = true, name = "Explore Tab")
@Composable
private fun MainScreenPreview_Explore() {
    MainScreen(component = PreviewRootComponent(MainTab.EXPLORE))
}

@Preview(showBackground = true, showSystemUi = true, name = "Categories Tab")
@Composable
private fun MainScreenPreview_Categories() {
    MainScreen(component = PreviewRootComponent(MainTab.CATEGORIES))
}
```

---

## 四、做完后怎么验证

### 4.1 编译检查

```bash
./gradlew :shared:compileKotlinAndroid
```

应该 0 错误。

### 4.2 运行检查

在模拟器上运行 App，确认：

1. **底部切 Tab 再切回"主页"** → 二级 Tab 状态保留（比如你选了"排行"，切去"收藏"再回来，仍然在"排行"）
2. **点二级 Tab（推荐/分类/排行）** → 下方列表文字变了（推荐 #1 → 分类 #1 → 排行 #1）
3. **旋转屏幕** → 状态不丢失

### 4.3 代码检查（自查清单）

| 检查项 | 怎么看 |
| --- | --- |
| HomeScreen 里没有 `mutableStateOf` | 搜索 `mutableStateOf`，HomeScreen.kt 里应该 0 结果 |
| HomeScreen 里没有 `placeholderComic` | 搜索 `placeholderComic`，HomeScreen.kt 里应该 0 结果 |
| 所有新文件都在 `commonMain` | HomeContract.kt / HomeComponent.kt 路径里都有 `commonMain` |
| HomeComponent 不在 Composable 里 new | 搜索 `DefaultHomeComponent(`，只在 HomeComponent.kt 和 SharedModule.kt 里出现 |

---

## 五、概念速查（遇到不懂的回来翻）

| 概念 | 一句话 | 类比你已知的 |
| --- | --- | --- |
| MVI | Model-View-Intent，单向数据流 | 类似 MVP 但更严格：View 只发 Intent、只读 State |
| State | 描述页面当前长什么样 | 类似 ViewModel 里的 UI State |
| Intent | 描述用户想干什么 | 类似 ViewModel 里的 fun 事件方法，但用 sealed interface 更安全 |
| Effect | 一次性事件（Toast/导航） | 类似 ViewModel 里的 SingleLiveEvent |
| Value / MutableValue | Decompose 的可观察状态容器 | 类似 StateFlow / MutableStateFlow |
| subscribeAsState() | 把 Value 转成 Compose State | 类似 collectAsState() |
| ComponentContext | Decompose 组件的生命周期上下文 | 类似 LifecycleOwner |
| childContext() | 给子组件创建独立上下文 | 类似 Fragment 的子 FragmentManager |
| KoinComponent | 让类具备 `get()` 注入能力 | 类似 Hilt 的 @Inject 入口 |
| `update { }` | 原子更新 MutableValue | 类似 StateFlow.update { } |

---

## 六、本步不做的事

- ❌ 不引入 Mock Repository / 网络 / Coil3（→ 3.3 步）
- ❌ 不接 Effect / Snackbar / Toast（HomeEffect 留空即可）
- ❌ 不抽 `core-common` 独立模块放 MVI 基类（先写在 shared 里，够用）
- ❌ 不评审 UI 细节（卡片样式、TabBar 样式都由你定）

---

## 七、3.3 步预告

3.3 步会做（了解即可，本步不用管）：
- 把 `HomeComponent.onIntent(SelectSubTab)` 里"直接写死生成列表"改为调用 Repository
- `HomeState` 加 `loadState: LoadState`（Idle/Loading/Success/Error），UI 三态
- 引入 `MockComicRepository` + `GetHomeListUseCase`
- 卡片接 Coil3 `AsyncImage`

本步的 `HomeState.comics: List<HomeComic>` 已经预留好了，3.3 步在外面包一层 LoadState 即可，不用改契约。

---

## 八、约定

- 本步阶段 A ✅ → 阶段 B（你写代码）
- 卡住贴报错 → 我进阶段 C 答疑（追加到 02-qa.md）
- 写完说 **`done`** / **`写完了`** → 我进阶段 D Review（只看逻辑，不评 UI）
