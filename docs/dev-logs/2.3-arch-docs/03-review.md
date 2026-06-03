# 第 2.3 步 Review — 架构文档回填

> 阶段 D 验收报告
> 验收时间：2026-06-03
> 范围：`docs/ARCHITECTURE.md` + `docs/MODULES.md` + 两份归档文件

---

## ✅ 通过项

| 检查点 | 验证方式 | 结果 |
| --- | --- | --- |
| `docs/ARCHITECTURE.md` 已落盘 | 文件存在 + 顶部标记完整 | ✅ |
| `docs/MODULES.md` 已落盘 | 文件存在 + 顶部标记完整 | ✅ |
| 当前快照模块清单与 `settings.gradle.kts` 一致 | `settings.gradle.kts` 实际包含 `:androidApp` + `:shared` 两个模块，与 MODULES.md 「当前快照」章节一致 | ✅ |
| 两份文档都标 `📌 快照截止步骤：2.2.b` | 两文件第 3 行均显示该标记 | ✅ |
| ARCHITECTURE ↔ MODULES 互相交叉引用 | ARCHITECTURE 顶部、§ 一、§ 二.1、§ 三.1 均有 → MODULES 跳转；MODULES 顶部、新增模块 checklist 均有 → ARCHITECTURE 跳转 | ✅（≥ 4 处） |
| 关键决策卡覆盖 ≥ 4 项 | ARCHITECTURE § 五 实际写了 6 项（D1 Decompose / D2 Koin / D3 SQLDelight / D4 Coil3 / D5 JS 引擎 / D6 Ktor） | ✅ |
| MVI 数据流图存在（Mermaid） | ARCHITECTURE § 三 包含 MVI Mermaid 流程图 | ✅ |
| 模块依赖 Mermaid 图存在（当前 + 目标） | MODULES 「当前快照」+「目标蓝图」均有 Mermaid graph | ✅ |
| 演进路线表步骤号与 ROADMAP 对齐 | 抽样核对：3.1 → feature-explore / 6.3 → data-favorites + core-database / 7.2 → core-storage / 8.1 → core-jsruntime，与 ROADMAP.md 步骤总览一致 | ✅ |
| 新增模块 checklist 完备 | MODULES 末尾 6 步 checklist 覆盖 settings.gradle.kts 注册 / build.gradle.kts 模板 / namespace / 依赖声明 / 文档同步 / Smoke 验证 | ✅ |
| docs 目录入口齐整 | `docs/` 现含 7 份顶层 md（README/DEVELOPMENT_RULES/ROADMAP/CHANGELOG/ARCHITECTURE/MODULES + dev-logs/），无缺漏 | ✅ |
| 不动业务代码原则 | 本步仅在 `docs/` 目录下增改，未触碰任何 `*.kt` / `*.kts` / `*.toml` | ✅ |

---

## ⚠️ 建议项（非阻塞，后续步骤可顺手补）

| 编号 | 建议 | 触发时机 |
| --- | --- | --- |
| S1 | `docs/README.md` 当前未列出 ARCHITECTURE.md 与 MODULES.md 的入口链接，建议在第 2.3 阶段 E 归档时顺手补一个「文档地图」小节 | 阶段 E 归档 |
| S2 | ARCHITECTURE § 二.1「三套源集职责」表中可考虑追加一行说明 `commonTest` / `androidHostTest`，目前只点到了 main 源集 | 第 11.3 步测试补齐时 |
| S3 | MODULES 「演进路线」表是按"引入步骤"排序的，未来步骤多了之后建议加一列「负责人/状态」，方便长期跟踪 | 第 6 步收藏页拆模块时 |
| S4 | ARCHITECTURE § 五 决策卡 D5（JS 引擎）目前是"三端不强求统一"的中性表述，第 8.1 步 AI 兜底时如果最终选定具体方案（Zipline vs QuickJS-Android），需要回填这张卡 | 第 8.1 步 |
| S5 | 两份文档未提到 ProGuard / R8 / iOS dSYM / Desktop 签名等"发布期"关注点，但这是 plan 第 10.3 / 11.3 步的范围，现在不补也合理 | 第 10.3 步 |

> 上述均为「未来回填点」，不阻塞当前步骤收尾。

---

## ❌ 阻塞项

无。

---

## Review 结论

**通过 ✅**，可进入阶段 E（归档）。

阶段 E 待办：

1. 更新 `docs/CHANGELOG.md` 追加 2.3 段（日期 + 文件清单 + 改动说明）
2. 更新 `docs/ROADMAP.md`，把 2.3 行从 `⏳` 打成 `✅`
3. 写 `docs/dev-logs/2.3-arch-docs/04-summary.md`（步骤总结 + 关键决策记录 + 学到的东西指引）
4. 顺手处理 S1（在 README.md 文档地图段落补 ARCHITECTURE/MODULES 链接）
