# 开发规范（DEVELOPMENT_RULES）

本文档为 xhunter 项目的强制规范，每次开发前请参考。AI 在 Code Review 时也会以此文档为依据。

> 规则只增不减；如有调整需求，先在本文档讨论修订，再落地代码。

---

## 1. 工具链与版本

| 项 | 要求 | 当前实际 |
| --- | --- | --- |
| Kotlin | 2.0+（K2 编译器） | 2.3.21 |
| AGP | 8.5+ | 9.0.1 |
| Gradle | 8.7+（Kotlin DSL） | 由 Wrapper 提供 |
| JDK（运行 Gradle 守护进程） | 17+ | 21（Amazon Corretto，由 `gradle/gradle-daemon-jvm.properties` 声明） |
| JVM 字节码目标 | 11+ | 11（`androidApp` 与 `shared`） |
| Compose Multiplatform | 1.7+ | 1.11.0 |
| Android compileSdk | 34+ | 36 |
| Android minSdk | 24 | 24 |
| Android targetSdk | 34+ | 36 |
| Xcode（iOS 启动后） | 15+ | — |

依赖统一在 `gradle/libs.versions.toml` 用 Version Catalog 管理，**禁止在子模块 `build.gradle.kts` 里硬编码版本号**。

---

## 2. 模块与目录约定

### 模块命名

```
composeApp / androidApp / iosApp / desktopApp        # 平台入口
shared-domain                                        # 业务域（UseCase/Entity/Repo Interface）
core-{common,designsystem,network,database,storage,jsruntime}
data-{comic,source-plugin,favorites,downloads}
feature-{explore,reader,favorites,downloads,settings,source-management}
```

> 当前向导生成的工程使用 `shared` + `androidApp` 两个模块，`composeApp` 概念尚未拆出来；将在第 2 步引入 feature/core 子模块时按上面规范命名。

### 包名根

`com.lwtor.xhunter.<module>.<layer>`，例如：
- `com.lwtor.xhunter.feature.explore.presentation`
- `com.lwtor.xhunter.data.comic.repository`
- `com.lwtor.xhunter.core.jsruntime`

### 平台代码隔离

- `commonMain/`：纯 Kotlin + Compose Multiplatform，**禁止**出现 Android/iOS/JVM 特有 API
- `androidMain/`：Android Framework 相关 actual 实现
- `iosMain/`：iOS CInterop 相关 actual 实现
- `desktopMain/` (jvmMain)：JVM 桌面端 actual 实现
- 所有平台差异通过 `expect`/`actual` 暴露在 `core-*` 模块，**禁止**在 `feature-*` 直接写平台代码

---

## 3. 架构原则（MVI + Clean）

### 分层依赖方向（绝不反向）

```
Platform Entry → composeApp → feature-* → shared-domain → data-* → core-*
```

### 核心约束

- `feature-*` 之间**不互相依赖**，跨 feature 通信走 domain UseCase 或导航参数
- ViewModel 只持有 `StateFlow<State>` 和 `SharedFlow<Effect>`
- 所有用户操作必须通过 `onIntent(intent: Intent)` 单一入口
- UseCase 是单一职责的纯函数封装，命名以动词开头：`GetComicDetailUseCase`、`ToggleFavoriteUseCase`
- Repository 接口定义在 `shared-domain`，实现放在 `data-*`

### MVI 状态约定

每个页面至少包含三态：

```kotlin
sealed interface XxxState {
    data object Loading : XxxState
    data class Success(val data: ...) : XxxState
    data class Error(val message: String) : XxxState
    data object Empty : XxxState   // 可选
}
```

---

## 4. 命名规范

| 对象 | 规范 | 示例 |
| --- | --- | --- |
| 类 / 接口 | 大驼峰 | `ComicRepository` |
| 函数 / 变量 | 小驼峰 | `getComicDetail()` |
| 常量 | 全大写下划线 | `MAX_PRELOAD_PAGES` |
| Composable | 大驼峰 + 名词 | `ComicCard`、`ExploreScreen` |
| Intent / Effect 子类 | 名词 / 动作短语 | `ExploreIntent.SwitchTab`、`DetailEffect.NavigateToReader` |
| 文件名 | 与主类同名 | `ComicRepository.kt` |
| 资源 ID（Android）| 蛇形 | `ic_back`、`bg_card` |

---

## 5. 编码风格

- 缩进：4 空格（Kotlin 官方默认），禁止 Tab
- 行宽：120 字符
- import：按 IDE 默认排序，禁止使用通配符 `*`
- 公共 API 必须有 KDoc，私有函数视复杂度而定
- 禁止 `!!`，用 `?.` / `?:` / `requireNotNull()`
- Composable 函数参数顺序：必填 → 可选 → `modifier: Modifier = Modifier` → lambda
- 严禁在 Composable 内做长耗时计算，必要时用 `remember` / `derivedStateOf`

### Lint / 格式化

- 使用 [ktlint](https://github.com/pinterest/ktlint) 或 IDE 内置 Reformat（`Cmd+Alt+L`）
- 提交前必须 `./gradlew ktlintCheck`（第 11 步会引入）

---

## 6. Git 规范

### 分支模型

- `main`：可发布分支，受保护
- `feature/<step-id>-<short-desc>`：每个步骤一个分支，合并后删除
  - 例：`feature/step-3-1-explore-ui`、`feature/step-5-2-reader-gesture`
- `fix/<short-desc>`：紧急修复

### Commit 信息

格式：`<type>(<scope>): <subject>`

| type | 含义 |
| --- | --- |
| feat | 新功能 |
| fix | bug 修复 |
| refactor | 重构（无功能变化） |
| docs | 仅文档 |
| chore | 构建 / 依赖 / 配置 |
| test | 测试 |
| style | 格式化 |

scope 用模块名（`explore` / `reader` / `core-jsruntime` 等）。

示例：
```
feat(explore): add bottom 4-tab navigation skeleton
fix(reader): correct double-tap zoom anchor point
docs(roadmap): update step 5 acceptance criteria
```

### Code Review

- 每个步骤完成后，发 PR 给 AI 做 Review
- AI Review 关注点：架构合规、命名一致、性能陷阱、平台差异处理
- 用户根据 Review 意见修改后再合并 main

---

## 7. 文档维护规范（强制）

每完成一个**大步骤**（1、2、3、…、11），必须按顺序：

1. 在 `docs/CHANGELOG.md` 追加一段：`## YYYY-MM-DD 第 X 步 <步骤名>`，下面列改动文件清单与简要说明
2. 如果引入了新模块，更新 `docs/MODULES.md` 对应章节
3. 如果改了架构决策（替换某依赖、调整分层），更新 `docs/ARCHITECTURE.md`
4. 如果学到 KMP 新知识点，在 `docs/LEARNING_NOTES.md` 追加一段（用户负责）

> ⚠️ **不允许跳过文档更新直接进下一步**。AI 每次接到下一步任务时会先检查上一步的 CHANGELOG 是否已追加。

---

## 8. AI 协作约定

### 用户的 5 环工作流

1. AI 先发任务文档 + 骨架代码（含 TODO 注释）
2. 用户照做实现
3. 卡住时贴报错/疑问给 AI
4. 完成后请求 Code Review
5. 追加 CHANGELOG

### AI 兜底范围

下列模块由 AI 写完整实现，用户负责阅读理解 + 调试：

- 阅读器手势（第 5 步）
- JS 引擎抽象层与各端 actual（第 8.1 / 9.2 / 10.2 步）
- 漫画源插件协议解析（第 8.2 步）
- 下载调度器（第 8.4 步）
- iOS / Desktop 平台 actual 集合（第 9.2 / 10.2 步）

其它模块都是用户主导，AI 只给骨架与关键片段。

### Spec 文档标准（教学级，强制）

AI 在阶段 A 输出 `01-spec.md` 时，必须遵循以下标准。旧版"只给契约签名 + 一句话描述"的 spec 不合格。

#### 1. 全局观优先

spec 开头必须有一节"这步在干什么"，包含：
- **现状 vs 目标**：用 ASCII 图或表格对比改动前后的代码结构，让用户一眼看出"要动哪些文件、哪些删、哪些加"
- **数据流图**：用箭头画出"用户操作 → Intent → Component → State → UI 重组"的完整链路，不要只写文字

#### 2. 子步骤必须完整可操作

每个子步骤必须包含四要素：
1. **做什么**：一句话说明本步目标
2. **完整代码**：给出可直接粘贴的完整文件内容（不是片段！不是只给函数签名！）
3. **关键解释**：对核心代码行给出"为什么要这么写"的解释 + 类比（优先类比 Android 已知概念，如 `MutableValue` ≈ `MutableStateFlow`）
4. **验证方式**：编译命令或运行检查项

#### 3. 修改型子步骤必须给出"当前代码 → 改成"

对已有文件的修改，必须同时展示：
- **当前代码**（改动前的完整代码块）
- **改成**（改动后的完整代码块）
- 用 `↓唯一改动↓` 注释标注改动的具体行

不能只说"在 XX 行加一行"，因为用户可能已经改过文件、行号对不上。

#### 4. 概念速查表

spec 末尾必须有"概念速查"表，每行格式：

| 概念 | 一句话解释 | 类比你已知的 |
| --- | --- | --- |

优先类比用户已学的 Android 概念（StateFlow、LifecycleOwner、ViewModel 等），让新概念有锚点。

#### 5. 验证检查清单

spec 必须包含"做完后怎么验证"一节，包含：
- 编译命令
- 运行时行为检查项（具体操作步骤 + 预期结果）
- 代码自查清单（搜什么关键词、应该 0 结果还是 N 结果）

#### 6. 不做的事 + 下步预告

- 明确列出"本步不做的事"（避免用户过度设计）
- 简短预告下一步做什么（1-2 句，让用户知道当前设计的兼容性边界）

### 提问规范

提问时尽量带：① 当前所在步骤编号（如 "在 3.2 步"）；② 完整报错堆栈或代码片段；③ 已经尝试过什么。AI 才能精准定位。

---

## 9. 安全与版权

- venera 是 GPL-3.0 许可，本项目作为学习用途同样以 GPL-3.0 开源
- **禁止**将本项目商用、上架应用商店、用于盈利
- **禁止**在仓库中提交任何漫画图片、JS 源文件（venera 的 jmcomic.js 等）等可能存在版权风险的内容
- 测试用 Mock 数据使用占位图（如 picsum.photos），不使用任何真实漫画封面
