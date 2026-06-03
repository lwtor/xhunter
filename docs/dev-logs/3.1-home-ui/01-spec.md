# 第 3.1 步：主页 Tab UI 骨架（HomeScreen）

> 阶段 A · 任务方向 + 步骤拆解 + 验收标准
> 创建时间：2026-06-03
> 归档目录：`docs/dev-logs/3.1-home-ui/`

---

## 0. 背景与边界澄清（重要）

### 0.1 「首页」「主页 Tab」是两个不同概念

经第 3.1 步前置对齐，**Plan 中的"首页"在本工程里被拆成两层**：

| 概念 | 含义 | 对应代码位置 |
| --- | --- | --- |
| **首页（外层 Shell）** | MainActivity 级别的外壳：底部 BottomBar + （未来）顶部 ToolBar + 中间内容容器，所有 Tab 共用 | `MainScreen.kt`（已存在，第 2.1 步落地） |
| **主页 Tab（HomeScreen）** | 4 个 Tab 之一，承载漫画内容浏览（顶部 TabBar 推荐/分类/排行 + 漫画卡片网格） | `ui/home/HomeScreen.kt`（**本步新增**） |

Plan 步骤总览表 3.1 行写的「首页 UI 骨架，顶部 TabBar(推荐/分类/排行) + 漫画卡片网格」**实际指主页 Tab 的内容**，不是外层 Shell。外层 Shell 已在第 2 大步完成。

### 0.2 4 Tab 命名修订（与原项目对齐验证后定稿）

第 2.1 步落地的 4 Tab 命名需要修订。已通过 web_fetch 拉取 venera 原项目 `lib/pages/main_page.dart` 验证：

| 序号 | 原项目英文 | 原项目 i18n 译法 | **本工程定名** | 当前枚举值 | 修订动作 |
| --- | --- | --- | --- | --- | --- |
| 1 | Home | 首页 | **主页** | `HOME` | label 改"主页"，枚举名保留 |
| 2 | Favorites | 收藏 | **收藏** | `FAVORITES` | 不变 |
| 3 | Explore | 探索 | **发现** | `EXPLORE` | label 改"发现"，枚举名保留 |
| 4 | Categories | 分类 | **分类** | （当前是 `PROFILE`）| **枚举值改名**：`PROFILE` → `CATEGORIES`，label "个人" → "分类"，icon 改 `Icons.Filled.Category` |

**没有"个人/我的"Tab**——venera 原项目也没有。设置/搜索等入口本步不做，留到后续步骤（plan 第 7 步设置；搜索独立步骤）。

### 0.3 与 plan 步骤总览表的字面差异

Plan 第 2.1 行写"4 个 Tab（首页/收藏/浏览/我的）"——这是 plan 早期信息架构，与原项目实际不符。本步按 venera 原项目 + 用户确认的最终定名（主页/收藏/发现/分类，无个人）执行，并在阶段 E 同步把 ROADMAP 第 2.1 行的 Tab 命名修正为最终命名（避免后续误导）。

---

## 1. 本步目标（看得见什么）

启动 App 后：

1. **底部 4 Tab** 的 label 与 icon 变成最终命名：主页 / 收藏 / 发现 / 分类
2. 默认进入 **主页 Tab**，可以看到：
   - 顶部一行 **TabBar**：推荐 / 分类 / 排行（3 个二级 Tab，**仅 UI**，点击可切换高亮，下方网格内容**写死不变**）
   - 下方 **漫画卡片网格**：2 列，写死 8 张占位卡，每张卡显示「占位封面框 + 标题 + 作者」
3. 切到收藏 / 发现 / 分类 Tab 时，仍然显示原来的 `当前 Tab: xxx` 占位文字（这 3 个 Tab 的真实内容留给 plan 后续步骤）

> 说明：本步只做"主页 Tab"的 UI 骨架，**不引入 ViewModel / 状态流 / Mock Repository**——这些是 3.2 / 3.3 的事。本步顶部 TabBar 切换可以用一个 `remember { mutableStateOf(...) }` 局部状态，下方网格数据用 commonMain 内的 `private val placeholderComics = listOf(...)` 写死。

---

## 2. 命名约定（与 0.2 节呼应）

### 2.1 枚举改动（精确到行）

`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainTab.kt`：

```diff
 enum class MainTab(
     val label: String,
     val icon: ImageVector,
 ) {
-    HOME(label = "首页", icon = Icons.Filled.Home),
-    FAVORITES(label = "收藏", icon = Icons.Filled.Favorite),
-    EXPLORE(label = "探索", icon = Icons.Filled.Search),
-    PROFILE(label = "个人", icon = Icons.Filled.Person),
+    HOME(label = "主页", icon = Icons.Filled.Home),
+    FAVORITES(label = "收藏", icon = Icons.Filled.Favorite),
+    EXPLORE(label = "发现", icon = Icons.Filled.Search),
+    CATEGORIES(label = "分类", icon = Icons.Filled.Category),
 }
```

> Icon 备选：`Icons.Filled.Search`（发现）/ `Icons.Filled.Explore`（发现备选）/ `Icons.Filled.Category`（分类）。如果你跑出来发现 `Icons.Filled.Category` 在 material-icons-extended 才有而当前没引入，可以临时用 `Icons.Filled.List` 或 `Icons.AutoMirrored.Filled.List` 替代，**等编译通过即可**，最终 icon 选型不在本步纠结。

### 2.2 涉及 RootComponent 的连锁修改

`RootComponent.kt` 当前 `private val _selectedTab = MutableValue(MainTab.HOME)` —— `MainTab.HOME` 还在所以**不用改**。

但是 `MainScreenPreview` 里的 `override val selectedTab: Value<MainTab> get() = MutableValue(MainTab.HOME)` 也保留，**Preview 不需要改**。

如果你的 IDE 报 `MainTab.PROFILE` 没找到，那是因为没有任何代码引用它了（已被 grep 验证），**直接改 enum 就好**。

---

## 3. 新增文件清单

### 3.1 主页 Tab 屏幕（本步核心）

**文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeScreen.kt`

**职责**：

- 顶部 TabBar：推荐 / 分类 / 排行（3 个 secondary tab，本步只做 UI，写死高亮逻辑）
- 中间漫画网格：`LazyVerticalGrid(columns = GridCells.Fixed(2))`，渲染 8 个 `ComicCard`
- 网格数据：文件内 private val 写死，例如：
  ```kotlin
  private data class PlaceholderComic(
      val id: String,
      val title: String,
      val author: String,
  )
  private val placeholderComics = List(8) { i ->
      PlaceholderComic(
          id = "demo-$i",
          title = "示例漫画 ${i + 1}",
          author = "作者 ${('A' + i)}",
      )
  }
  ```

**结构骨架**（伪代码层面）：

```
HomeScreen()
├── Column
│   ├── HomeTopTabBar(selected, onSelectedChange)        // 顶部 3 Tab
│   └── ComicGrid(comics, modifier = Modifier.weight(1f)) // 网格
│
├── private @Composable HomeTopTabBar(...)               // ScrollableTabRow 或 TabRow 都行
└── private @Composable ComicGrid(comics)                // LazyVerticalGrid
```

> 顶部 TabBar 用 `androidx.compose.material3.TabRow` + `Tab`。

### 3.2 卡片组件（本步核心）

**文件**：`shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/components/ComicCard.kt`

**职责**：

- 单张漫画卡片
- 入参：标题 String + 作者 String + onClick lambda（本步可空实现）
- UI：
  - 顶部封面占位（`Box` + `MaterialTheme.colorScheme.surfaceVariant` 背景 + 2:3 宽高比，**本步不接图片库**）
  - 下方两行文字：标题（最多 2 行 ellipsis）+ 作者（1 行 ellipsis，字号小、颜色次要）
- 卡片整体用 `Card` 或 `Surface` + `clickable {}`

> Coil3 图片加载是 3.3 步的事，本步封面就是一个纯色 Box，**不要在 build.gradle 里加 coil 依赖**。

### 3.3 包层级建议

```
ui/
├── main/                  # Shell 层（已存在）
│   ├── MainScreen.kt
│   ├── MainTab.kt
│   └── RootComponent.kt
└── home/                  # 主页 Tab（本步新增）
    ├── HomeScreen.kt
    └── components/
        └── ComicCard.kt
```

> 现在还没拆 feature-home 模块（plan 里 `feature-explore` 是目标蓝图，本步快照仍是单 shared 模块）。包路径用 `ui.home`，未来拆模块时整体平移到 `feature-home/.../ui/`，迁移成本可控。

---

## 4. MainScreen 接线（精确改动）

`MainScreen.kt` 当前中间内容区域是一个单一的 `Box { Text("当前 Tab: ...") }`，**本步要按 `selected` 分发**：

```diff
     ) { innerPadding ->
         Box(
             modifier = Modifier.fillMaxSize().padding(innerPadding)
         ) {
-            Box(
-                modifier = Modifier.fillMaxSize().align(Alignment.Center)
-            ) {
-                Text(
-                    modifier = Modifier.align(Alignment.Center),
-                    text = "当前 Tab: ${selected.label}"
-                )
-            }
+            when (selected) {
+                MainTab.HOME -> HomeScreen()
+                MainTab.FAVORITES,
+                MainTab.EXPLORE,
+                MainTab.CATEGORIES -> Box(
+                    modifier = Modifier.fillMaxSize(),
+                    contentAlignment = Alignment.Center,
+                ) {
+                    Text(text = "当前 Tab: ${selected.label}")
+                }
+            }
         }
     }
```

> 别忘了 import：`com.lwtor.xhunter.ui.home.HomeScreen`。

> 暂用 `when` + 简单分支即可。Plan 第 2.2 步引入 Decompose ChildStack 时本步代码会被替换成 `Children(stack)` 形式——这是 plan 后续要做的事，**本步不要提前重构**。

---

## 5. 步骤拆解（建议编码顺序）

| # | 动作 | 影响文件 | 验收信号 |
| --- | --- | --- | --- |
| 1 | 修改 `MainTab.kt` 的 4 个 label + 把 `PROFILE` 改名 `CATEGORIES` + icon 调整 | `MainTab.kt` | 编译通过；底部 Tab 显示「主页/收藏/发现/分类」 |
| 2 | 新建 `ui/home/components/ComicCard.kt` | 新增 1 文件 | Preview 能看到一张占位卡（如果你写了 Preview） |
| 3 | 新建 `ui/home/HomeScreen.kt`，包含顶部 TabBar + LazyVerticalGrid | 新增 1 文件 | Preview 能看到「3 个顶部 Tab + 8 张卡 2 列网格」 |
| 4 | 改 `MainScreen.kt` 中间区域，按 `selected` 分发 | `MainScreen.kt` | 跑模拟器，主页 Tab 看到网格；其他 3 Tab 仍显示「当前 Tab: xxx」 |
| 5 | 跑 Android 模拟器手动验收 | - | 见第 6 节 |

---

## 6. 验收清单

跑 `./gradlew :androidApp:installDebug` 启动后：

- [ ] 底部 4 个 Tab：**主页 / 收藏 / 发现 / 分类**（label 全对，无"个人"）
- [ ] 默认进主页 Tab
- [ ] 主页 Tab 顶部有 3 个 Tab：推荐 / 分类 / 排行；点击可切换高亮，下方网格不变（本步预期行为）
- [ ] 主页 Tab 下方是 2 列网格 × 4 行 = 8 张卡片
- [ ] 每张卡片：上半占位色块（约 2:3 宽高比）+ 标题 + 作者（占位文字）
- [ ] 切到 收藏/发现/分类 Tab，显示对应的「当前 Tab: 收藏 / 发现 / 分类」
- [ ] 切回主页 Tab，顶部二级 Tab 高亮状态**可重置或保持均可**（本步不强制要求保持，因为没有 ViewModel）
- [ ] 没有 lint 错误（`read_lints` 或 AS Inspection 通过）
- [ ] 没有 crash 与红屏

---

## 7. 本步**明确不做**的事（避免越界）

- ❌ 不做主页 ViewModel / Intent / State / Effect（→ 3.2）
- ❌ 不做 MockComicRepository / UseCase / domain 接口（→ 3.3）
- ❌ 不接 Coil3，不做封面图加载，不做 loading/empty/error 三态（→ 3.3）
- ❌ 不引入 Decompose ChildStack（plan 第 2 大步外，本步保持现状即可）
- ❌ 不做搜索 / 设置 / 历史等入口（plan 后续步骤）
- ❌ 不拆 feature-home 模块（保持 shared 单模块快照）
- ❌ 不为顶部二级 Tab 切换添加业务逻辑——只做 UI 层的 selected 高亮

---

## 8. 风险点 & 注意事项

| 风险 | 概率 | 处理方式 |
| --- | --- | --- |
| `Icons.Filled.Category` 在当前依赖中不存在 | 中 | 临时用 `Icons.AutoMirrored.Filled.List` 或别的现成 icon，记一下 LEARNING_NOTES，3.x 步骤再补 material-icons-extended 依赖 |
| `LazyVerticalGrid` import 路径混淆 | 低 | 用 `androidx.compose.foundation.lazy.grid.LazyVerticalGrid` + `GridCells.Fixed(2)` |
| Preview 无法渲染 LazyVerticalGrid | 低 | 在 Preview 里给 `Modifier.height(600.dp)` 限制高度 |
| MainScreen 中间分发后 padding 重复 | 低 | `innerPadding` 应用在最外层 Box，`HomeScreen()` 内部自己用 `fillMaxSize()` 即可 |
| 本步把顶部二级 Tab state 用 `remember`，3.2 重写时要丢弃 | 低 | spec 已显式提示这是临时方案，3.2 会迁到 ViewModel |

---

## 9. 与 plan/Roadmap 的同步动作（阶段 E 再做，本节仅备忘）

阶段 E 归档时：

1. `docs/CHANGELOG.md` 追加「2026-06-XX 第 3.1 步 主页 Tab UI 骨架」段落
2. `docs/ROADMAP.md`：
   - 把 3.1 行从 ⏳ → ✅，补「实际产物」「快照机制」两栏
   - **修订 2.1 行**：把 Tab 命名「首页/收藏/浏览/我的」→「主页/收藏/发现/分类」，并在备注里加一句"原 plan 早期描述与原项目验证后修订"
   - 当前进度行同步打 ✅ 到 3.1
3. `docs/MODULES.md`：在「演进路线 → 第 3 步」处更新「3.1 完成」标记；包路径表追加 `ui/home/`
4. `docs/dev-logs/3.1-home-ui/04-summary.md`：步骤总结
5. （可选）`docs/ARCHITECTURE.md`：本步没有架构层变化，**不动**

---

## 10. 阶段约定

- 阶段 A（本文）输出后，AI 进入**等待**状态，不动业务代码
- 用户编码（阶段 B）按第 5 节顺序进行；过程中遇到问题贴报错/疑问，AI 在阶段 C 解答（追加写 `02-qa.md`）
- 用户说「done」/「写完了」/「ok 了」/「提交了」→ 进入阶段 D Review
- Review 通过 → 阶段 E 归档 → AI 提示说 `commit` → 阶段 F 预览 → 用户说 `push` → 阶段 G 提交 → 阶段 H 等下一步

---

> 准备好就动手吧。卡住随时贴报错，我接阶段 C。
