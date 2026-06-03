# 第 2.2.a 步：Code Review

> 阶段 D / Review 验收
> Review 日期：2026-06-03
> Reviewer：AI

---

## 一、Review 总览

| 维度 | 结果 |
|---|---|
| 功能验收 | ✅ 全部通过（5/5） |
| 代码质量 | ⚠️ 2 项建议（不阻塞） |
| 阻塞项 | ❌ 0 项 |
| **总评** | **通过，可进入阶段 E 归档** |

---

## 二、✅ 通过项

### 验收项对照（spec §四）

| 编号 | 验收项 | 验证 |
|---|---|---|
| AC-1 | 4 Tab 切换正常 | ✅ MainScreen 通过 `component.onTabSelected(tab)` 改变状态，`subscribeAsState` 驱动重组 |
| AC-2 | 旋转屏幕保留选中态 | ✅ `defaultComponentContext()` 自动接入 `savedStateRegistry` + `lifecycle`，且创建在 `super.onCreate` 之前（关键时机正确） |
| AC-3 | Lint 0 报错 | ✅ `read_lints` 全文件 0 diagnostics |
| AC-4 | App.kt 不再持有 selected 状态 | ✅ `App.kt` 只有 `MaterialTheme + MainScreen(rootComponent)` 两行；`MainScreen.kt` 已无 `rememberSaveable` |
| AC-5 | RootComponent 概念上可单测 | ✅ `RootComponent` 接口与 `DefaultRootComponent` 实现都不依赖任何 Compose API（纯 `Value<T>` + `MutableValue<T>`） |

### 代码亮点

#### 1. `RootComponent.kt` —— 接口/实现分离做得很标准

```kotlin
interface RootComponent {
    val selectedTab: Value<MainTab>
    fun onTabSelected(tab: MainTab)
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext {
    private val _selectedTab = MutableValue(MainTab.HOME)
    override val selectedTab: Value<MainTab> = _selectedTab
    override fun onTabSelected(tab: MainTab) { _selectedTab.value = tab }
}
```

- ✅ `MutableValue` 私有 + `Value` 对外只读 —— 完美对应 `MutableStateFlow` / `StateFlow` 的封装范式
- ✅ `ComponentContext by componentContext` 类委托用得对，为后续 `childContext("xxx")` 留好了通道
- ✅ 构造函数注入 `componentContext`（不是 new），符合 Decompose 宪法

#### 2. `MainActivity.kt` —— 时机修正后非常干净

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    val root: RootComponent = DefaultRootComponent(
        componentContext = defaultComponentContext(),
    )
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { App(rootComponent = root) }
}
```

- ✅ `defaultComponentContext()` 在 `super.onCreate` **之前**调用，Decompose 官方推荐写法
- ✅ 上方代码注释把"为什么这样写"也写清楚了，未来回看不会忘
- ✅ `val root: RootComponent` 显式声明接口类型而不是实现类，调用方依赖抽象（小细节但很职业）

#### 3. `MainScreenPreview.kt`（androidMain）—— Preview 跨模块方案落地

- ✅ 4 个 Tab 各起一个 Preview，命名规范（`MainScreenPreview_Home/Favorites/Explore/Profile`），IDE 渲染后能一字排开看
- ✅ `PreviewRootComponent` 是 `private class`，业务代码不会误用
- ✅ `onTabSelected = Unit` 显式说明"Preview 不响应交互"
- ✅ 文件级 KDoc 解释了为什么放 androidMain，未来队友能秒懂

#### 4. `App.kt` —— 删除 `@Preview` 注解的决策正确

```kotlin
@Composable
fun App(rootComponent: RootComponent) {
    MaterialTheme { MainScreen(component = rootComponent) }
}
```

- ✅ 带必填 RootComponent 参数的 Composable 不能直接 `@Preview`，转移到 `MainScreenPreview.kt` 的方案到位
- ✅ App 层职责单一：MaterialTheme 套壳 + 委托给 MainScreen

#### 5. 依赖配置

- ✅ `libs.versions.toml` 的 `decompose = 3.2.2` 与 spec 锁定版本一致
- ✅ `shared/build.gradle.kts` commonMain 加了 `decompose` + `decompose.compose`
- ✅ `androidApp/build.gradle.kts` 加了 `implementation(libs.decompose)` 让 `defaultComponentContext()` 可用
- ✅ 没有引入 `essenty`（避开了 spec §五 Pit 1 提到的版本冲突坑）

---

## 三、⚠️ 建议项（不阻塞，可以现在改也可以拖到 2.2.b 一起改）

### 建议 1：删掉 `MainScreen.kt` 里残留的旧 `MainScreenPreview()`（推荐立刻改）

**位置**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` 第 60-73 行

```kotlin
@Preview                          // ← androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MainScreenPreview() {
    MainScreen(
        component = object : RootComponent {
            override val selectedTab: Value<MainTab>
                get() = MutableValue(MainTab.HOME)
            override fun onTabSelected(tab: MainTab) { }
        },
    )
}
```

连带：第 15 行 `import androidx.compose.ui.tooling.preview.Preview`、第 17-18 行 `MutableValue` / `Value` import 也要一起删（如果 MainScreen 函数本体没用到）。

**为什么应该删**：

1. **职责重复**：`androidMain/MainScreenPreview.kt` 已经有 4 个更完整的 Preview，这个孤立的 Preview 是上一轮迁移的残留
2. **commonMain 不该引 androidx 注解**：虽然 Compose Multiplatform 1.7+ 给 `androidx.compose.ui.tooling.preview.Preview` 在 commonMain 提供了 expect 别名（所以现在能编译），但语义上它**只在 Android 端被 IDE 渲染器读**，commonMain 引用属于"能跑但不规范"。等 9.x/10.x 步上 iOS/Desktop 时这种残留会成为隐性技术债
3. **和 spec §六明确边界冲突**：spec 写明"Preview 移到 androidMain"，本文件保留旧 Preview 等于这条规则没贯彻

**改动量**：删 14 行 + 调整 import，2 分钟搞定。

> 按规则 AI 不动业务代码，请你自己删。删完打开 `MainScreen.kt` 看右栏 Preview 缩略图是否还在（应该还在 —— IDE 会展示 `androidMain/MainScreenPreview.kt` 里 4 个）。

---

### 建议 2：`MainScreen.kt` 内层 Box 嵌套冗余（小，可选）

**位置**：第 44-55 行

```kotlin
Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
    Box(modifier = Modifier.fillMaxSize().align(Alignment.Center)) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "当前 Tab: ${selected.label}"
        )
    }
}
```

两层 Box 嵌套可以扁平为一层：

```kotlin
Box(
    modifier = Modifier.fillMaxSize().padding(innerPadding),
    contentAlignment = Alignment.Center,
) {
    Text("当前 Tab: ${selected.label}")
}
```

- 性能层面节省一个 Layout 节点
- `contentAlignment = Alignment.Center` 比手动 `.align()` 更声明式

**优先级**：低。本步占位文案 2.3 之后就被首页真实内容替换，留着也行。如果你想顺手练一下 Compose Modifier，可以改。

---

## 四、❌ 阻塞项

无。

---

## 五、对 spec 的反向修订记录

回顾 spec / 答疑过程中我提的几条**错误信息**，做正式纠错登记，会一起进 04-summary.md：

| 错误来源 | 内容 | 正确做法 |
|---|---|---|
| spec §三 Step 5 | 把 `defaultComponentContext()` 写在 `super.onCreate(savedInstanceState)` 之后 | 必须在 `super.onCreate` **之前**调用 |
| 答疑过程中 | 一度建议把 `MainTab` 枚举值改成驼峰 `Home/Favorites` | Kotlin 枚举常量按规范本来就该大写 `HOME/FAVORITES`，保留你的原写法正确 |
| 答疑首版 Preview 方案 | 只在 androidApp 写 Preview | 体验差（跨模块跳）；改进方案：放 `shared/androidMain` 让 IDE 自动关联到 `MainScreen.kt` 旁边 |

这些都已在 02-qa.md 里有过临时记录，归档时再统一整理。

---

## 六、下一步

1. **可选改动**：处理上面建议 1（强烈建议）+ 建议 2（看心情）
2. **进入阶段 E**：我接着写 04-summary.md + 更新 CHANGELOG.md / ROADMAP.md，把 2.2.a 这一步彻底归档
3. **节奏控制权在你**：归档完我会停下等你说"开始 2.2.b"

---

**Review 结论：✅ 通过**。可以进入阶段 E 归档。
