# 第 2.1 步 — 主页框架：底部 4 Tab UI 骨架

> 阶段 A 开发文档 / 创建于 2026-06-02

---

## 一、本步目标（看得见什么）

App 启动后**不再**显示 KMP 向导的 "xhunter + Click me" 默认页，而是显示一个带 **底部 4 Tab 导航** 的主页框架：

```
+--------------------------------+
|                                |
|                                |
|       当前 Tab 占位内容          |
|     (比如 "首页 - TODO")        |
|                                |
|                                |
+--------------------------------+
|  首页  |  收藏  |  浏览  |  我的  |
+--------------------------------+
```

（实际 UI 用 Material3 `NavigationBar`，每个 Tab 带图标 + 文字，选中态自动高亮；上图仅为布局示意）

- 4 个 Tab：**首页 / 收藏 / 浏览 / 我的**
- 点击底部 Tab 切换内容区
- 每个 Tab 内容区**只显示一行占位文字**（如 `"首页 - TODO"`），不做任何业务 UI
- 选中的 Tab 在底部高亮（Material3 默认样式即可）
- 旋转屏幕（横竖屏切换）后，**当前选中的 Tab 不丢失**

---

## 二、本步**不**做的事（边界）

明确划清，避免越界：

- ❌ **不**接入 Decompose（路由 / 返回栈 / 状态保存机制）→ 第 2.2 步
- ❌ **不**接入 Koin（依赖注入）→ 第 2.2 步
- ❌ **不**做 ViewModel / MVI（State/Intent/Effect）→ 第 3.2 步起
- ❌ **不**做任何业务功能（漫画卡片、列表、详情）→ 第 3 步起
- ❌ **不**拆分 `core-designsystem` 模块（先继续在 `shared/commonMain` 里写，到第 2.2/2.3 拆模块时再迁移）
- ❌ **不**改主题/配色（KMP 向导给的 `MaterialTheme` 默认色就够了）
- ❌ **不**写图标资源（用 `Icons.Filled.Home` 等 Material 自带的就够，**不**需要导入第三方图标库）

---

## 三、技术要点速查

### 1) Material3 `NavigationBar`

Compose Multiplatform 的 `material3` 自带 `NavigationBar`，写法：

```kotlin
NavigationBar {
    NavigationBarItem(
        selected = 当前是否选中,
        onClick = { /* 切换状态 */ },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
        label = { Text("首页") },
    )
    // ... 其他 3 个 Item
}
```

整体结构用 `Scaffold(bottomBar = { NavigationBar { ... } }) { padding -> ... }` 包起来，content 区会自动避开底部栏的空间。

### 2) 状态保存（横竖屏不丢）

只用 Compose 自带能力即可，**不引 Decompose**：

```kotlin
var selectedTab by rememberSaveable { mutableIntStateOf(0) }
```

- `rememberSaveable` 而不是 `remember` —— 关键区别！`rememberSaveable` 会把状态写进 `Bundle`，旋转屏幕重建 Activity 后能恢复
- `mutableIntStateOf` 是 Compose 1.6+ 推荐写法（避免 `mutableStateOf<Int>` 的装箱）

### 3) 图标来源

用 Compose Material 自带 `androidx.compose.material.icons.Icons.Filled.*` 就够：

- 首页：`Icons.Filled.Home`
- 收藏：`Icons.Filled.Favorite`
- 浏览：`Icons.Filled.Search` 或 `Icons.Filled.Explore`
- 我的：`Icons.Filled.Person` 或 `Icons.Filled.AccountCircle`

> ⚠️ Material **图标库**默认只随 `material-icons-core` 提供少数图标（Home/Favorite/Search/Person/Settings 等）。我们这里要的都在核心包里，**不需要**额外加 `material-icons-extended` 依赖。

### 4) 包结构建议

在 `shared/src/commonMain/kotlin/com/lwtor/xhunter/` 下新增子目录 `ui/main/`：

```
shared/src/commonMain/kotlin/com/lwtor/xhunter/
├── App.kt                      ← 改造：把 hello 内容换成 MainScreen 调用
├── Greeting.kt                 ← 保留不动（向导默认产物）
├── GreetingUtil.kt             ← 保留不动
├── Platform.kt                 ← 保留不动
└── ui/
    └── main/
        ├── MainScreen.kt       ← 新增：装 Scaffold + NavigationBar 的入口
        └── MainTab.kt          ← 新增：枚举类，定义 4 个 Tab 元数据
```

> 暂时不放在 `core-designsystem` 模块，原因：第 2.1 步不拆模块。这个目录第 2.3 步规划时会重新评估去向。

---

## 四、详细任务拆解（你需要做的事）

### 任务 1：定义 4 Tab 枚举

**新建文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainTab.kt`

要求：定义一个枚举（`enum class`）描述 4 个 Tab 的元数据，每个 Tab 至少包含：

- `label: String`（显示在底栏的文字，如 `"首页"`）
- `icon: ImageVector`（Material Icon）

> 提示：`ImageVector` 类型来自 `androidx.compose.ui.graphics.vector.ImageVector`。

为什么用枚举而不是 4 个独立 `@Composable`？因为：
- 数量固定、有序、互斥（一次只能选一个） → 枚举的天然场景
- `NavigationBar` 渲染时可以 `MainTab.entries.forEach { ... }` 一行循环
- 后续第 2.2 步 Decompose 路由要用到时，枚举可以直接当 key

### 任务 2：写 MainScreen

**新建文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt`

要求：
- 一个 `@Composable fun MainScreen()`
- 内部用 `rememberSaveable` 保存当前选中 Tab
- `Scaffold` 包裹，`bottomBar` 放 `NavigationBar`，`content` 区按当前 Tab 显示对应占位文字
- 占位文字居中显示（`Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(...) }`）
- 占位文字格式：`"${tab.label} - TODO"`，比如选中收藏时显示 `"收藏 - TODO"`

### 任务 3：改造 App.kt

把 `App.kt` 里的 hello 模板内容**整体替换**成 `MainScreen()` 调用，**保留** `MaterialTheme { ... }` 包裹：

```kotlin
@Composable
@Preview
fun App() {
    MaterialTheme {
        MainScreen()
    }
}
```

> 旧的 `Greeting()` / `painterResource` / `compose_multiplatform` 图片相关 import 都可以删掉。`Greeting.kt` / `GreetingUtil.kt` 文件本身**先留着**（暂时无害，后续清理），第 2.1 步不动它们。

### 任务 4：跑模拟器验收

```
点 AS 工具栏 ▶️ 跑 androidApp，模拟器应能看到底部 4 Tab，可切换。
```

---

## 五、验收标准（阶段 D Review 时按这条核对）

- [ ] **AC1**：App 启动后默认显示首页 Tab（`selectedTab = 0`），底栏首页项高亮
- [ ] **AC2**：依次点击「首页 / 收藏 / 浏览 / 我的」4 个 Tab，内容区文字相应变化（`首页 - TODO` → `收藏 - TODO` → `浏览 - TODO` → `我的 - TODO`）
- [ ] **AC3**：每次点击后，底栏只有当前 Tab 高亮，其他 3 个为未选中态
- [ ] **AC4**：旋转屏幕（模拟器 `Ctrl + ←/→` 或 `Cmd + ←/→`），当前选中的 Tab **不变**（验证 `rememberSaveable` 生效）
- [ ] **AC5**：4 个 Tab 都有图标 + 文字双行显示（Material3 默认样式）
- [ ] **AC6**：代码层面 — `MainTab` 是枚举且字段命名清晰；`MainScreen` 函数无业务逻辑；`App.kt` 只剩 `MaterialTheme { MainScreen() }`
- [ ] **AC7**：编译无 warning（除 KMP 向导自带的 lifecycle 版本告警等无关项）

---

## 六、关键概念速查（第一次接触 KMP/CMP 看这里）

### `@Composable` 函数

声明式 UI 函数，描述"画什么"而不是"怎么画"。每次状态变化，Compose 自动决定哪些 `@Composable` 需要重新执行（"重组 / recomposition"）。

### `Modifier`

修饰符链，控制布局/大小/点击/绘制等。链式调用，**顺序敏感**：
```kotlin
Modifier.padding(16.dp).background(Color.Red)  // padding 在外，红色在内
Modifier.background(Color.Red).padding(16.dp)  // 红色在外，padding 在内（内容区缩进）
```

### `Scaffold`

Material3 的标准布局骨架，提供 `topBar` / `bottomBar` / `floatingActionButton` / `content` 等命名插槽（slot），自动处理状态栏、导航栏的 inset。

### `Scaffold` 的 padding 参数

`Scaffold` 的 `content` lambda 会接收一个 `PaddingValues`：

```kotlin
Scaffold(bottomBar = { NavigationBar { ... } }) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        // 这里的内容会自动避开底栏
    }
}
```

**一定要用** `innerPadding`，否则你的内容会被底栏遮住。

### `remember` vs `rememberSaveable`

| | `remember` | `rememberSaveable` |
|---|---|---|
| 重组（recomposition）后保留 | ✅ | ✅ |
| 配置变更（旋转屏幕）后保留 | ❌ | ✅ |
| 进程死亡恢复 | ❌ | ✅（Bundle 序列化） |

第 2.1 步的 Tab 状态用 `rememberSaveable`。

### Compose Multiplatform vs Jetpack Compose

`shared/commonMain` 里写的 `@Composable` 函数（包来自 `org.jetbrains.compose.material3.*` 而不是 `androidx.compose.material3.*`），可以同时跑在 Android / iOS / Desktop / Web。当前 `App.kt` 已经是 CMP 写法，所以你新增的 `MainScreen` 直接照抄 imports 风格就行。

---

## 七、踩坑预警

1. **`Icons.Filled.Home` 找不到**：检查 import，应是 `androidx.compose.material.icons.Icons` + `androidx.compose.material.icons.filled.Home`（注意是 `material.icons` 不是 `material3.icons`，Material 图标在所有 Material 版本里共用）
2. **`NavigationBarItem` 不显示文字**：必须传 `label = { Text(...) }`，且 `NavigationBar` 默认 `alwaysShowLabel = true`，没设置则只在选中时显示
3. **占位文字被底栏遮住**：忘了用 `Scaffold` 给的 `innerPadding`
4. **Tab 切换后选中状态不变**：检查 `selected = (selectedTab == tab.ordinal)` 这种比较是否正确；或检查 `onClick` 是否更新了 state
5. **`mutableIntStateOf` 报错没找到**：需要 import `androidx.compose.runtime.mutableIntStateOf`，Compose 1.6+ 才有

---

## 八、本步预计耗时

熟手 30 分钟，新手 1-2 小时（含查 NavigationBar API + 调试旋转屏幕）。

---

## 九、阶段 C 答疑入口

写代码过程中任何疑问，直接来问我：

- 不会写某段代码 → 我在消息里贴可参考片段（不写文件）
- 报错 → 把错误信息整段贴过来
- 不知道哪种写法更好 → 我对比说明利弊

我会把每次问答按时间顺序追加到本目录下的 `02-qa.md`。

---

## 十、阶段流转

```
你现在在 → 阶段 B（用户编码）
完成后说 "done" / "写完了" / "提交了" / "ok 了" → 进入阶段 D（Review）
```

阶段 B 期间我**不动任何代码文件**，只追加 `02-qa.md` 答疑。
