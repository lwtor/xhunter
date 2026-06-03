# 第 2.2.a 步：归档总结

> 阶段 E / 文档更新与归档
> 归档日期：2026-06-03
> 状态：✅ 完成

---

## 一、本步交付物

### 新增源码

| 路径 | 作用 |
|---|---|
| `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/RootComponent.kt` | Decompose 根 Component：`RootComponent` 接口 + `DefaultRootComponent` 实现，承载 `selectedTab: Value<MainTab>` 状态 |
| `shared/src/androidMain/kotlin/com/lwtor/xhunter/ui/main/MainScreenPreview.kt` | androidMain 跨模块 Preview，4 Tab 各一份，使用私有 `PreviewRootComponent` 假数据 |

### 修改源码

| 路径 | 改动 |
|---|---|
| `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` | 签名改为 `MainScreen(component: RootComponent)`，删除 `rememberSaveable` 自管理状态，改为订阅 `component.selectedTab.subscribeAsState()` + 调 `component.onTabSelected()` |
| `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt` | 签名改为 `App(rootComponent: RootComponent)`，去掉 `@Preview`，把根 Component 透传给 `MainScreen` |
| `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt` | `onCreate` 中先调 `defaultComponentContext()`（在 `super.onCreate` 之前）创建 `DefaultRootComponent`，再 `setContent { App(rootComponent = root) }` |

### 依赖与构建

| 路径 | 改动 |
|---|---|
| `gradle/libs.versions.toml` | 新增 `decompose = "3.2.2"` 版本 + `decompose` / `decompose-extensions-compose` 两个 library 别名 |
| `shared/build.gradle.kts` | `commonMain.dependencies` 追加 `decompose` + `decompose.extensions.compose` |
| `androidApp/build.gradle.kts` | `dependencies` 追加 `implementation(libs.decompose)`，让 `defaultComponentContext()` 可见 |

### 文档归档

| 路径 | 作用 |
|---|---|
| `docs/dev-logs/2.2-a-decompose/01-spec.md` | 阶段 A 任务说明 |
| `docs/dev-logs/2.2-a-decompose/02-qa.md` | 阶段 C 答疑（追加式） |
| `docs/dev-logs/2.2-a-decompose/03-review.md` | 阶段 D Review 报告 |
| `docs/dev-logs/2.2-a-decompose/04-summary.md` | 本文（阶段 E） |

---

## 二、功能变化（用户视角）

- 旋转屏幕（横竖屏切换）后，**当前选中 Tab 不丢失**（验收已通过）
- App 启动入口从"自管理状态的 MainScreen"切换为"由 Decompose RootComponent 驱动状态"
- 视觉效果与 2.1 完全一致（占位文案 + Tab 切换），但底层架构已为后续 Decompose `ChildStack`（详情页、阅读器内嵌导航）打好基础

---

## 三、新接触的知识点

1. **Decompose 心智模型**
   - Component 是 ViewModel + Navigation 的合体：既持有状态，又决定子层级路由
   - 不需要 ViewModel 框架；状态用 `Value<T>` / `MutableValue<T>` 表达（类似 `StateFlow` / `MutableStateFlow`）
   - `ComponentContext` 是 Decompose 的"上下文"：承载 lifecycle、savedState、backHandler、instanceKeeper —— 由父级传给子级，根 Component 由 Activity/iOS Window 提供（`defaultComponentContext()`）

2. **类委托模式 `class X : Y by y`**
   - `class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, ComponentContext by componentContext`
   - 让 `DefaultRootComponent` 自动获得 `ComponentContext` 的全部方法实现，无需手写转发
   - 后续 `childContext("xxx")` 直接可用，是 Decompose 嵌套子组件的关键

3. **Decompose 与 Compose 的桥接**
   - `Value<T>.subscribeAsState()` 由 `decompose-extensions-compose` 提供，把 Decompose 的 `Value` 转成 Compose 的 `State`
   - 关键：Component 本身完全不依赖 Compose，**可以单测**（不需要 Robolectric / Compose Testing）

4. **`defaultComponentContext()` 的调用时机（坑点）**
   - 必须在 `super.onCreate(savedInstanceState)` **之前**调用
   - 因为 `defaultComponentContext()` 内部要 hook 进 `savedStateRegistry`，错过 `super.onCreate` 的注册窗口就拿不到状态恢复
   - 这条 spec 第一版写错了（写在 super 之后），由用户在编码时发现，已在 review §五登记

5. **commonMain ↔ androidMain Preview 拆分策略**
   - 带必填参数的 Composable 不能直接 `@Preview`（IDE 不知道怎么构造）
   - 解法：在 `shared/src/androidMain/.../MainScreenPreview.kt` 写 Preview，构造一个私有 `PreviewRootComponent` 假实现
   - IDE 因为 `androidMain` 跟 `commonMain` 同模块，能在 `MainScreen.kt` 旁边渲染出预览缩略图
   - 比"Preview 写在 androidApp 模块"体验好很多（不用跨模块跳）

---

## 四、对 spec 的反向修订（错误纠正登记）

| 错误内容 | 来源 | 正确做法 |
|---|---|---|
| `defaultComponentContext()` 写在 `super.onCreate(savedInstanceState)` 之后 | spec §三 Step 5 第一版 | 必须在 `super.onCreate` **之前**调用，否则 savedStateRegistry hook 失败 |
| 一度建议把 `MainTab` 枚举值改驼峰 `Home/Favorites` | 答疑过程 | Kotlin 枚举常量规范是大写 `HOME/FAVORITES`，保留原写法 |
| 第一版 Preview 方案放在 `androidApp` 模块 | 答疑首版 | 改进方案放 `shared/src/androidMain`，IDE 能就近渲染，体验更好 |

---

## 五、遗留项（带到后续步骤处理）

### TODO-2.2-1：清理 `MainScreen.kt` 残留旧 `MainScreenPreview()`

- **位置**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` 第 60-73 行
- **症状**：上一版 Preview 用 `androidx.compose.ui.tooling.preview.Preview` 写在 commonMain，已被 `androidMain/MainScreenPreview.kt` 完全替代但旧代码未删
- **影响**：不阻塞当前功能；commonMain 引 androidx 注解属"能跑但不规范"，未来上 iOS/Desktop 时是隐性技术债
- **建议处理时机**：在 2.2.b（Koin DI）这一小步顺手清理，或下次任意涉及 MainScreen 的改动时一起删
- **改动量**：删 14 行 + 调整 import，2 分钟

### TODO-2.2-2（可选）：`MainScreen.kt` 内层 Box 嵌套扁平化

- **位置**：第 44-55 行
- **现状**：两层 Box 嵌套用 `align(Alignment.Center)` 居中
- **建议**：扁平为单层 `Box(contentAlignment = Alignment.Center)`，节省一个 Layout 节点
- **优先级**：低。占位文案到 2.3 后就被真实内容替换，可不改

---

## 六、下一步

- **2.2.b**（计划中）：Koin DI 接入
  - `androidApp/MyApplication.kt`（新建）启动 `startKoin`
  - 把 `RootComponent` 注入交给 Koin（替代 `MainActivity` 里 `DefaultRootComponent(...)` 的 new）
  - 验证日志打印 Koin 注入成功
- **节奏**：等用户说"开始 2.2.b"或"下一步"再启动阶段 A

---

✅ **2.2.a 归档完成**。
