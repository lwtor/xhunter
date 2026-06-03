# 第 2.3 步 · 文档回填 — Spec

> 阶段 A 产出。本步不动业务代码，仅在 `docs/` 下产出两份架构文档。

## 步骤目标

为 xhunter 项目补齐两份架构文档，让后续读者（包括未来的自己）能从文档进入项目，而不是从代码反推：

- `docs/ARCHITECTURE.md` —— 架构思想 / MVI 数据流 / KMP 心智 / 关键决策卡
- `docs/MODULES.md` —— Gradle 模块视图 / 依赖图 / 每模块卡片 / 新增模块 checklist

## 关键决策

### 决策 1：双视角并存（已拍板）

两份文档都同时呈现「当前快照」+「目标蓝图」+「演进路线」三个视角：

- **当前快照**：完全对照真实代码（截止 2.2.b 只有 `androidApp` + `shared` 两个 Gradle 模块）
- **目标蓝图**：plan 里的全模块依赖图（composeApp / core-* / data-* / feature-* 等）
- **演进路线**：哪一步会拆出哪个模块（与 ROADMAP 编号对齐）

理由：xhunter 是学习项目，文档要承担「学习路径锚点」职责；纯快照失全局视野，纯蓝图与代码脱节。

### 决策 2：两份文档分工

| 文档 | 定位 | 主要内容 |
| --- | --- | --- |
| `ARCHITECTURE.md` | **思想层** | MVI 数据流图、KMP expect/actual 心智、Clean Architecture 分层原则、关键技术决策卡 |
| `MODULES.md` | **工程层** | settings.gradle.kts 模块清单、模块依赖 Mermaid 图、每模块卡片（职责/依赖/关键文件）、新增模块 checklist、演进路线表 |

两者交叉引用：ARCHITECTURE 提到模块时跳 MODULES；MODULES 提到分层原则时跳 ARCHITECTURE。

### 决策 3：快照机制

- 两份文档顶部都标 **`📌 快照截止步骤：2.2.b（Koin DI 完成）`**
- 后续每次拆模块（例如 3.1 引入 `feature-explore`），把"更新这两份文档"作为该步骤阶段 E 的必做项
- 避免文档每次小改都要回头补全部模块，也避免文档腐化

### 决策 4：不与 plan 重复

- 技术细节（库版本、JS 引擎选型理由等）锚定 plan，docs 里只写「是什么 + 为什么这么分」
- 决策卡只列结论 + 一行理由 + 锚点链接，不展开论证过程

## 涉及文件改动

### 新增

- `docs/ARCHITECTURE.md`
- `docs/MODULES.md`

### 修改（阶段 E 归档时动）

- `docs/CHANGELOG.md` — 追加 2.3 段
- `docs/ROADMAP.md` — 把 2.3 行打 ✅

### 归档目录（按阶段产出）

- `docs/dev-logs/2.3-arch-docs/01-spec.md`（本文，阶段 A）
- `docs/dev-logs/2.3-arch-docs/02-qa.md`（阶段 C 累加）
- `docs/dev-logs/2.3-arch-docs/03-review.md`（阶段 D）
- `docs/dev-logs/2.3-arch-docs/04-summary.md`（阶段 E）

### 严格不动

- 任何 `*.kt` / `*.kts` / `*.toml` / `*.gradle` / `*.xml` 等业务代码
- `settings.gradle.kts` 只读，用来对照写 MODULES.md

## 步骤拆解

| 阶段 | 内容 | 谁动手 |
| --- | --- | --- |
| A 开发文档 | 出 spec（本文） | AI |
| B "用户编码" | 本步无编码，AI 直接起草两份文档草稿 | AI |
| C 答疑 | 用户审阅草稿提调整意见 → AI 改稿，过程写进 02-qa.md | AI 改 / 用户审 |
| D Review | 用户说 done → AI 跑文档自检，出 03-review.md | AI |
| E 归档 | 更新 CHANGELOG / ROADMAP / 写 04-summary.md | AI |
| F commit 预览 | 走 commit 两步约定 | AI |
| G push | 走 push 约定 | AI |
| H 等下一步 | AI 等待 | — |

> 注：本步是文档型步骤，阶段 B 由 AI 直接起草草稿，与 2.x.b 类「用户写代码」相反。这是 plan 里既定的「AI 出文档草稿，用户检查并补充」分工。

## 验收标准

- ✅ `docs/ARCHITECTURE.md` 存在
  - 含 MVI 数据流图（Mermaid）
  - 含 KMP 心智段落（commonMain / androidMain / iosMain / desktopMain 职责 + expect/actual 用法）
  - 含 Clean Architecture 分层原则段落
  - 含至少 4 项关键决策卡（Decompose / Koin / SQLDelight / Coil3 等，对齐 plan 决策清单）
  - 顶部含 `📌 快照截止步骤：2.2.b`
  - 至少一处链接到 `MODULES.md`
- ✅ `docs/MODULES.md` 存在
  - 含「当前快照」章节：模块清单与 `settings.gradle.kts` 一致（仅 `:androidApp` + `:shared`）
  - 含「目标蓝图」章节：plan 全模块依赖 Mermaid 图
  - 含「演进路线」表：步骤号 → 引入的模块（与 ROADMAP 对齐）
  - 含每模块卡片：职责 / 依赖 / 关键文件 / 引入步骤
  - 含「新增模块 checklist」（建子目录、改 settings、配 build.gradle.kts、加版本目录、回填本文件）
  - 顶部含 `📌 快照截止步骤：2.2.b`
  - 至少一处链接到 `ARCHITECTURE.md`
- ✅ 两份文档相互引用
- ✅ 演进路线编号与 ROADMAP 一致（如「3.1 → 引入 feature-explore（首次出现 feature 层）」）
- ✅ 阶段 E 完成 CHANGELOG 追加段 + ROADMAP 打 ✅

## 与之前协作模式的差异

这是文档型步骤，由 AI 直接起草两份文档草稿，**用户角色变为审阅 + 提改稿意见**，与 2.x.b 类的「AI 不动业务代码」相反。但其他阶段（C/D/E/F/G/H）流程不变。
