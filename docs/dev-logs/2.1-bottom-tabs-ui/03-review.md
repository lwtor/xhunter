# 第 2.1 步 — Review 验收

> 阶段 D / 验收时间：2026-06-03 10:57
> Reviewer：AI

---

## 一、总体结论

**✅ 通过**。可进入阶段 E（文档归档）。

无 ❌ 阻塞项；2 个 ⚠️ 建议项（非必改，但建议吸收，下一步前修订）。

---

## 二、验收标准核对（对照 `01-spec.md` 第五节）

| 编号 | 验收点 | 结果 | 证据 |
|---|---|---|---|
| AC1 | 启动默认首页 Tab，底栏首页项高亮 | ✅ | `MainScreen.kt:24` `mutableStateOf(MainTab.Home)`；enum 第一项即 Home |
| AC2 | 切 4 Tab 内容文字相应变化 | ✅ | `MainScreen.kt:40` `"${selectedTab.label} - TODO"` |
| AC3 | 仅当前 Tab 高亮 | ✅ | `MainScreen.kt:54` `selected = tab == selected` |
| AC4 | 旋转屏幕选中 Tab 不丢失 | ✅ | `MainScreen.kt:24` 用 `rememberSaveable`；`MainTab` 是 enum，JVM 自动实现 `Serializable` |
| AC5 | 4 Tab 图标 + 文字双行 | ✅ | `MainScreen.kt:56-58` 同时传 `icon` + `label`，`alwaysShowLabel = true` |
| AC6 | 代码结构 — `MainTab` 是 enum、`MainScreen` 无业务、`App.kt` 仅留 `MaterialTheme { MainScreen() }` | ✅ | 三份文件均符合 |
| AC7 | 编译无新增 warning | ✅ | `read_lints` 通过，0 报错 0 警告 |

7/7 全部通过。

---

## 三、✅ 做得好的地方

1. **抽出 `MainBottomBar` 私有 Composable**：主体 `MainScreen` 一眼看清结构，第 2.2 步接 Decompose 改动面会很小
2. **`MainTab` 用 enum + `entries`**：`MainTab.entries.forEach { tab -> ... }` 是 Kotlin 1.9+ 推荐写法（不用旧版 `values()`），零开销
3. **`Modifier` 参数从外部传入**：`fun MainScreen(modifier: Modifier = Modifier)` 符合 Compose 官方规范，调用方可注入额外约束
4. **`contentDescription` 复用 `tab.label`**：无障碍朗读直接念中文 Tab 名，比传 `null` 更负责
5. **`@Preview` 加在 `MainScreenPreview` 上而不是 `MainScreen`**：让 Preview 函数与生产函数解耦，避免 Preview 注解被业务调用方继承
6. **依赖配置正确**：本步前 Q2 答疑追加的 `material-icons-core` 依赖已生效，`Icons.Filled.*` 全部 resolve

---

## 四、⚠️ 建议项（非阻塞，可选修订）

### ⚠️ 建议 1：占位 `Text` 未居中

**位置**：`MainScreen.kt:35-41`

**现状**：
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
) {
    Text(text = "${selectedTab.label} - TODO")
}
```

`Box` 默认对齐是 `Alignment.TopStart`（左上角），所以现在 "首页 - TODO" 这行字会贴在屏幕左上、紧挨状态栏。

`01-spec.md` 第 136 行明确要求："占位文字居中显示（`Box(modifier = ..., contentAlignment = Alignment.Center) { Text(...) }`）"。

**建议改法**（一行）：
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    contentAlignment = Alignment.Center,   // ← 加这行
) {
    Text(text = "${selectedTab.label} - TODO")
}
```

需要追加 import：`androidx.compose.ui.Alignment`。

**为什么算建议而不是阻塞**：占位文字本身就是临时的，第 3 步会被首页真实内容替换；但 spec 写到了就值得对齐，不然后续看 spec 的人会困惑。

---

### ⚠️ 建议 2：Tab 命名与 spec 措辞不一致

**位置**：`MainTab.kt:16-17`

**现状**：
```kotlin
Explore(label = "探索", ...)
Profile(label = "个人", ...)
```

**spec 措辞**（`01-spec.md` 第 9、26、166 行多处）：
- "浏览" Tab → 你写成了 "探索"
- "我的" Tab → 你写成了 "个人"

**问题**：意思一致（用户感知无差），但 ROADMAP / 后续 dev-log 里其他文档也会用 "浏览" / "我的" 这两个词，留着不改未来文档对照容易迷惑。

**建议改法**：二选一
- 选项 A（推荐）：把代码改成 `Explore(label = "浏览", ...)` / `Profile(label = "我的", ...)`，对齐 spec
- 选项 B：保留代码不动，在 `01-spec.md` 顶部加一行说明 "实际命名以代码为准：浏览=探索 / 我的=个人"，并在阶段 E 同步到 ROADMAP

**为什么算建议而不是阻塞**：用户感知层面没问题，纯粹是文档/代码措辞统一性。**你拍板**用哪个。

---

### ⚠️ 建议 3（极轻）：函数体首尾空行冗余

**位置**：`MainScreen.kt:23、43`

```kotlin
fun MainScreen(modifier: Modifier = Modifier) {
                                                ← 这行空行多余
    var selectedTab by ...
    Scaffold(...) { ... }
                                                ← 这行空行也多余
}
```

**建议**：删掉函数体首尾的空行，符合 Kotlin 官方代码风格（`ktlint` / `detekt` 默认会报）。

完全是审美层面，不改也行。

---

## 五、❌ 阻塞项

无。

---

## 六、阶段流转

```
当前 → 阶段 D 通过
下一步 → 阶段 E（AI 自动进入）：
  1) 更新 docs/CHANGELOG.md（追加 2.1 完成条目）
  2) 更新 docs/ROADMAP.md（标记 2.1 为 ✅）
  3) 写 04-summary.md（步骤总结 + 学到的关键概念）
  4) 完成后停下，等用户说"开始第 2.2 步"
```

> 上面 3 条 ⚠️ 建议，**强烈推荐至少修建议 1**（spec 明确要求居中），建议 2/3 看你心情；如果你想先修再归档，告诉我"等下，我先改"，我等；如果你说"直接归档"，我立刻进阶段 E。
