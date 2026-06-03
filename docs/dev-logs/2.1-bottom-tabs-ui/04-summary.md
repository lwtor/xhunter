# 第 2.1 步 — 步骤总结

> 阶段 E 归档 / 完成于 2026-06-03

---

## 一、做了什么（一句话）

把 KMP 向导的 "Click me" 默认页换成 **`Scaffold` + `NavigationBar` 的底部 4 Tab 主页框架**，并打通 `rememberSaveable` 保存选中 Tab 的能力。

## 二、产物清单

### 新增代码
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainTab.kt`（19 行）
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt`（69 行）

### 改造代码
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt` — 移除 hello 模板，简化为 `MaterialTheme { MainScreen() }`

### 依赖配置
- `gradle/libs.versions.toml` 追加 `compose-material-icons-core = "1.7.3"`
- `shared/build.gradle.kts` `commonMain.dependencies` 追加 `implementation(libs.compose.material.icons.core)`

### 文档
- `docs/dev-logs/2.1-bottom-tabs-ui/01-spec.md`（开发文档）
- `docs/dev-logs/2.1-bottom-tabs-ui/02-qa.md`（3 次问答：MainTab 写法 / Icons 找不到 / MainScreen 写法）
- `docs/dev-logs/2.1-bottom-tabs-ui/03-review.md`（Review 7/7 通过）
- `docs/dev-logs/2.1-bottom-tabs-ui/04-summary.md`（本文件）

## 三、关键学习要点

### 1. `Scaffold` 的 `innerPadding` 必须用

```kotlin
Scaffold(bottomBar = { ... }) { innerPadding ->
    Box(Modifier.fillMaxSize().padding(innerPadding)) { ... }
    //                          ^^^^^^^^^^^^^^^^^^^^^^^ 不加这行底栏会盖内容
}
```

`Scaffold` 帮你算好了"避开顶/底栏"的 padding，但传给你的 lambda 时不会自动应用——要你自己 `.padding(innerPadding)` 一下。这是 Compose 新人最常踩的坑。

### 2. `rememberSaveable` vs `remember`

| 维度 | `remember` | `rememberSaveable` |
|---|---|---|
| 重组保留 | ✅ | ✅ |
| 配置变更（旋转屏幕） | ❌ | ✅ |
| 进程被回收后恢复 | ❌ | ✅（走 Bundle） |

第 2.1 步必须用 `rememberSaveable` 保 Tab 状态，否则旋转屏幕 Tab 回弹首页。

**enum 自动可保存**：Kotlin enum 在 JVM 上自动实现 `java.io.Serializable`，可直接被 `rememberSaveable` 保存，无需写 `Saver`。如果第 3.2 步起改用 sealed class 或 data class 表示状态，需要 `Saver` 或 `@Parcelize`；但那时换 ViewModel 的 `StateFlow + SavedStateHandle` 后此问题自动消失。

### 3. Material Icons 在 CMP 是独立依赖

CMP 的 `compose.material3` 不会传递引入 Material Icons，需要单独加：

```toml
compose-material-icons-core = { module = "org.jetbrains.compose.material:material-icons-core", version = "1.7.3" }
```

- `material-icons-core`：~30 个核心图标（Home/Favorite/Search/Person 等），APK 体积几乎 0 影响 → 第 2.1 步用这个
- `material-icons-extended`：2000+ 图标，APK +~10MB → 后续遇到核心集没有的图标再加，**不要**为了"以后可能用到"提前引入

### 4. `MainTab.entries` 是 Kotlin 1.9+ 写法

```kotlin
MainTab.entries.forEach { tab -> ... }   // ✅ 推荐，零开销属性
MainTab.values().forEach { tab -> ... }  // ⚠️ 旧版，每次复制数组
```

我们项目 Kotlin 2.3.21，直接用 `entries`。

### 5. 业务 Composable 不包 `MaterialTheme`

主题由调用方（`App.kt`）统一提供：

```kotlin
@Composable
fun App() {
    MaterialTheme {
        MainScreen()   // 内部直接用 MaterialTheme.colorScheme.*，不再嵌套 MaterialTheme
    }
}
```

这是 Compose 标准约定，避免主题嵌套与重复声明。

### 6. 抽 `MainBottomBar` 是为了未来不动 UI

```kotlin
@Composable
private fun MainBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) { ... }
```

第 2.2 步接 Decompose 时，`onSelect` 会改成 `component.onTabSelected(it)`，**只动 `MainScreen` 一处接线，`MainBottomBar` 完全不动**。这就是把"事件回调"抽到 Composable 参数的复用红利。

## 四、Review 反馈

7/7 验收标准全过，0 ❌ 阻塞，3 ⚠️ 建议（用户决定不改，理由：占位文字临时性、Tab 命名"探索/个人"与"浏览/我的"语义等价、空行风格无影响）。详见 `03-review.md`。

## 五、回顾：本步在路线图里的位置

```
第 1 步 ✅  项目搭建（Hello xhunter）
第 2 步     主页框架
   ├─ 2.1 ✅ 底部 4 Tab UI 骨架  ← 当前完成
   ├─ 2.2    Decompose + Koin
   └─ 2.3    架构文档回填
第 3 步起   各业务页面...
```

## 六、给第 2.2 步的预备提示

进入 2.2 时，AI 在阶段 A 会重点说明：

1. **Decompose `RootComponent` / `ChildStack`**：把 `MainScreen` 里的 `var selectedTab` 替换成 `component.activeChild`，物理返回键能回到上一个 Tab
2. **Koin Module 模板**：Application/MainActivity 启动 Koin，给 `RootComponent` 注入 `ComponentContext`
3. **MVI 基类雏形**：在 `core-common`（暂时仍在 `shared/commonMain` 内）准备 `MviIntent` / `MviState` / `MviEffect` 接口，第 3.2 步起首页 ViewModel 用上
4. **状态保存升级**：`rememberSaveable` 让位给 Decompose 的 `StateKeeper`，演示"为什么单 Activity 多 Component 比 Compose 自带保存机制更适合复杂导航"

## 七、本步收尾

✅ 已通过 Review
✅ CHANGELOG 已追加
✅ ROADMAP 已标记 2.1 完成
✅ `04-summary.md` 已写入

**等待用户指令**："开始第 2.2 步" / "下一步" / "继续" 任一即可触发阶段 A。
