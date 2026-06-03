# 第 3.1 步总结（阶段 E 归档）

> 完成时间：2026-06-03
> 步骤名：首页 UI 骨架（含主页 Tab 命名修订）
> 归档目录：`docs/dev-logs/3.1-home-ui/`

---

## 一、本步做了什么（逻辑视角）

> ⚠️ UI 视觉细节本步起不再纳入 Review 范围（参见 [03-review.md] 协作边界声明），本总结只描述逻辑/结构层面发生的变化。

### 1. `MainTab` 枚举重构

- `PROFILE` 改名为 `CATEGORIES`，枚举值数量保持 4 个不变
- 4 个 label 按用户最终决策落地：主页 / 收藏 / 探索 / 分类（Tab 文案最终归属用户判断，AI 不干预）
- 图标选型：`CATEGORIES` 使用 `Icons.Filled.Menu` 兜底（`Icons.Filled.Category` / `Icons.Filled.GridView` 在当前 compose 版本的 core 包均不存在，详见 [02-qa.md]）
- `MainScreen.kt` 内部 `when (selected)` 分支也同步换成新枚举值，无残留旧引用

### 2. `MainScreen.kt` 内容区分发逻辑落地

原 2.x 步实现：内容区是单一占位 Text，无视当前 Tab。

本步实现：

```kotlin
when (selected) {
    MainTab.HOME -> HomeScreen()
    MainTab.FAVORITES,
    MainTab.EXPLORE,
    MainTab.CATEGORIES -> { /* 占位 Text */ }
}
```

- **HOME 分支**：调用本步新增的 `HomeScreen()`，作为 3.x 系列的主页落点
- **其余 3 个分支**：继续走占位 Text 路径，等 6.x（收藏）/ 后续 plan 步骤接管

### 3. `ui/home/HomeScreen.kt` 新增（首页落点确立）

- 新建 `com.lwtor.xhunter.ui.home` 子包，与 `ui.main` 平级
- `HomeScreen()` 是无参 Composable，内部状态用 `remember { mutableStateOf(...) }` 临时持有（符合 spec "3.1 步暂不引 ViewModel，3.2 步再迁移" 的约定）
- 占位漫画数据用 `List(8) { ... }` 在文件内 private 域生成，承担"卡片数量真实"这件事，等 3.3 步换 Mock Repository 时整体被替换
- UI 长什么样、几列、间距、卡片是否独立成文件 = 用户视觉判断，本归档不评

---

## 二、协作边界变化记录（本步开始生效）

本步阶段 D 第二轮 Review 期间，用户明确告知协作边界调整：

> "UI 细节不需要你处理了，这部分我自己来抉择就可以，你需要是负责逻辑部分"

后续所有阶段（spec / qa / review / summary）严格遵守：

| 范围 | AI 是否介入 |
| --- | --- |
| 编译错误 / KMP commonMain 平台 API 隔离 | ✅ |
| 状态流 / MVI 接线 / Decompose Component / Koin DI | ✅ |
| 模块依赖方向 / expect-actual 边界 | ✅ |
| 资源泄漏 / 生命周期 / 并发 | ✅ |
| 布局结构 / 视觉细节 / 文案 / 命名风格 / 文件拆分粒度 | ❌ |

详细规则见 [03-review.md] 顶部「协作边界声明」段落。

---

## 三、Review 关键发现与处理

### B1（已修复）：KMP commonMain 引 JDK 内部 API

`MainScreen.kt:20` 出现 `import com.sun.tools.javac.Main`（IDE 自动补全错位），违反 KMP commonMain 平台无关原则——iOS/Desktop 编译必爆炸。

**处理**：用户删除该 import，第二轮 Review 通过。

**沉淀经验**（写进自己的脑子）：

- KMP commonMain 严禁出现 `com.sun.*` / `sun.*` / `javax.*` 之外的 JVM 私有包
- IDE 自动 import 时盯一下顶部 import 块，尤其是常见短名（Main / Context / Type / List）容易被错配到 JDK/Android 内部
- detekt-rule `ForbiddenImport` 后续可以加上 `com.sun.*` 进黑名单（plan 第 11.3 步打磨阶段处理）

### S1（未修复，留到下次顺手清）

`MainTab.kt:7` 残留 `import androidx.compose.material.icons.filled.Person`（PROFILE 删除后未清理）。属代码卫生，不阻塞，下次 Optimize Imports 时一起清。

---

## 四、产物清单

| 文件 | 状态 | 说明 |
| --- | --- | --- |
| `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainTab.kt` | 修改 | `PROFILE → CATEGORIES`，icon 改 `Icons.Filled.Menu` |
| `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` | 修改 | 内容区改成 `when (selected)` 分发，HOME → `HomeScreen()` |
| `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeScreen.kt` | 新增 | 首页落点，无参 Composable + 占位数据 + 二级 Tab 状态（`remember`） |
| `docs/dev-logs/3.1-home-ui/01-spec.md` | 新增 | 阶段 A spec |
| `docs/dev-logs/3.1-home-ui/02-qa.md` | 新增 | 两次图标找不到的 QA（Q1 GridView 也没有 / Q2 改用 Menu） |
| `docs/dev-logs/3.1-home-ui/03-review.md` | 新增 | 两轮 Review；第二轮按新协作边界重写 |
| `docs/dev-logs/3.1-home-ui/04-summary.md` | 新增 | 本文件 |

---

## 五、对后续步骤的影响

### 3.2 步（首页 ViewModel + MVI）需要做的迁移

- `HomeScreen` 当前的 `remember { mutableStateOf(HomeSubTab.RECOMMEND) }` 临时状态要迁到 `HomeViewModel` / Component
- 顶部二级 Tab 切换从"UI 自管理 mutableStateOf"改成"Intent → State 驱动"
- 占位数据 `placeholderComics` 从文件 private 域**不要**直接迁移，应在 3.2 步进 ViewModel `initialState`，3.3 步再下沉到 Repository

### 3.3 步（Mock Repository + Coil3）需要做的事

- 真正引入 `data-comic` 模块（plan 中第 3.3 步引入，本步未引）
- 引入 `coil3` + `coil3-network-ktor` 依赖
- 占位数据从文件 private 域 → MockComicRepository → UseCase → ViewModel `state`

### plan/Roadmap 不需要本步动

主页 Tab 第 3 个 label 实际叫"探索"还是"发现"是 UI 文案决策（非逻辑），本步不再回填 ROADMAP。当前 ROADMAP 第 2.1 行的"首页/收藏/浏览/我的"老命名也按 UI 细节归属用户处理，AI 不主动改。

---

## 六、下一步

阶段 E 完成。按协作约定，AI 现在主动提示进入阶段 F：

> 请说 **`commit`** → 我执行 `git status` + `git diff --stat`，输出待提交清单 + 改动量 + 拟用 commit message 作为预览，等待你确认。
