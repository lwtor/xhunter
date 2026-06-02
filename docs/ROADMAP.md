# ROADMAP — 开发路线图

> 本路线图按"看得见的页面"切步骤，每个页面再拆成 **a) UI 骨架 → b) ViewModel(MVI) → c) 数据层(Mock/真实)** 三小步。所有页面先用 Mock 数据做完，最后第 8 大步统一接入真实 JS 漫画源。

## 全景概览

| 大步 | 主题 | 平台 | 看得见什么 |
| --- | --- | --- | --- |
| 1 | 项目搭建（极简） | Android | 空白页"Hello xhunter" |
| 2 | 主页框架 | Android | 底部 4 Tab 切换 |
| 3 | 首页 | Android | 漫画网格 + Coil 加载封面 |
| 4 | 详情页 | Android | 列表→详情完整跳转 |
| 5 | 阅读器 [AI 兜底] | Android | 翻页 + 缩放 + 预加载 |
| 6 | 收藏页 | Android | 分组管理 + SQLDelight 持久化 |
| 7 | 下载/设置/我的 | Android | 三个 Tab 完整可用 |
| 8 | 接入真实 JS 源 [AI 兜底] | Android | 真实漫画站可搜可读可下 |
| 9 | iOS 铺开 | + iOS | iOS 模拟器全功能 |
| 10 | Desktop 铺开 | + macOS/Win | dmg/exe 可分发 |
| 11 | 打磨 | 全平台 | 对齐 venera v1.6.3 |

---

## 详细步骤表

### 第 1 步：项目搭建（极简）

| 字段 | 内容 |
| --- | --- |
| 目标 | Android 模拟器跑出页面显示 "xhunter" 标题 + 点击按钮显示 "Hello xhunter on Android!" |
| AI 角色 | 出向导参数清单 + libs.versions.toml 模板 + README/DEVELOPMENT_RULES/ROADMAP/CHANGELOG 四份文档 |
| 用户角色 | 用 [kmp.jetbrains.com](https://kmp.jetbrains.com) 生成脚手架 → AS 打开 → 改 App.kt 显示 xhunter → Run |
| 涉及模块 | shared / androidApp |
| 完成验收 | 模拟器顶部显示 "xhunter"，按钮点击显示 Compose Logo + "Compose: Hello xhunter on Android!"；docs/ 下 4 份文档存在 |

---

### 第 2 步：主页框架（拆 3 小步）

#### 2.1 底部 4 Tab UI 骨架

| 字段 | 内容 |
| --- | --- |
| 目标 | 底部 4 Tab（首页/收藏/浏览/我的）切换正常，每个 Tab 显示占位文字 |
| AI 角色 | 出 `MainScreen` Composable 骨架 + Tab 数据类 |
| 用户角色 | 实现 Scaffold + NavigationBar 容器 |
| 涉及模块 | composeApp / core-designsystem |
| 完成验收 | 4 个 Tab 可点击切换，每个 Tab 中央显示对应名字 |

#### 2.2 Decompose 路由 + Koin DI

| 字段 | 内容 |
| --- | --- |
| 目标 | 4 Tab 切换有返回栈、状态保存；Koin Module 跑通 |
| AI 角色 | 出 `RootComponent` / `ChildStack` 骨架 + Koin Module 模板 |
| 用户角色 | 接线 Component 与 ViewModel；在 Application/MainActivity 启动 Koin |
| 涉及模块 | composeApp / core-common（MVI 基类） |
| 完成验收 | 切到 Tab2 → 系统返回键能回到 Tab1；Koin 注入打 log 验证 |

#### 2.3 文档回填

| 字段 | 内容 |
| --- | --- |
| 目标 | docs/ARCHITECTURE.md + docs/MODULES.md 完成（带模块依赖 Mermaid 图） |
| AI 角色 | 出文档草稿 |
| 用户角色 | 检查并补充 |
| 涉及模块 | docs/ |
| 完成验收 | 两份文档存在并能在 GitHub 渲染 Mermaid |

---

### 第 3 步：首页（拆 3 小步）

#### 3.1 UI 骨架

| 字段 | 内容 |
| --- | --- |
| 目标 | 顶部 TabBar（推荐/分类/排行）+ 漫画卡片网格（写死 8 张占位卡） |
| AI 角色 | 出 `ExploreScreen` 骨架 + `ComicCard` 组件 |
| 用户角色 | 调整网格列数/间距、ComicCard 内的标题/作者层级 |
| 涉及模块 | feature-explore / core-designsystem |
| 完成验收 | 首页 Tab 显示 8 张写死的卡片网格 |

#### 3.2 ViewModel + MVI

| 字段 | 内容 |
| --- | --- |
| 目标 | 顶部 Tab 切换会改变下方网格内容（仍是写死数据，但通过 State 驱动） |
| AI 角色 | 出 `ExploreIntent`/`State`/`Effect` + ViewModel 骨架 |
| 用户角色 | 实现 `onIntent` 处理 + Composable 订阅 State |
| 涉及模块 | feature-explore |
| 完成验收 | 切顶 Tab，网格内容变化（State 驱动） |

#### 3.3 Mock 数据层 + Coil3 封面

| 字段 | 内容 |
| --- | --- |
| 目标 | 卡片显示真实网络图片（Coil3 加载占位 URL），有 loading/empty/error 三态 |
| AI 角色 | 出 `MockComicRepository` + `ComicRepository` 接口 + Coil3 配置 |
| 用户角色 | 实现 `GetExploreListUseCase`、UI 三态分支 |
| 涉及模块 | feature-explore / shared-domain / data-comic |
| 完成验收 | 首屏看到 Coil 加载占位图（picsum.photos）；模拟下拉刷新能看到 loading 圈 |

---

### 第 4 步：详情页（拆 3 小步）

#### 4.1 UI 骨架

目标：点击首页卡片进入详情页，显示封面 + 标题 + 作者 + 简介 + 章节列表  
AI：`ComicDetailScreen` 骨架（CollapsingToolbar）  
用户：实现折叠 AppBar + 章节 LazyColumn  
完成验收：点卡片进详情页，能看到全部静态布局

#### 4.2 ViewModel

目标：详情页有 loading/error/success 三态，点章节有反馈  
AI：`DetailIntent`/`State` + ViewModel 骨架  
用户：实现状态处理  
完成验收：进入瞬间 loading；2 秒后切到 success；故意触发 error 能回退

#### 4.3 Mock 数据层 + 跳转

目标：列表→详情完整跳转，详情数据从 Mock Repository 拉取  
AI：`GetComicDetailUseCase` + Mock 数据 + 导航参数定义  
用户：接线导航参数传递  
完成验收：从首页 8 张卡片任选一张点进，看到对应漫画的详情数据

---

### 第 5 步：阅读器 [AI 兜底]（拆 3 小步）

#### 5.1 UI 骨架 — AI 写完整实现

目标：全屏阅读器：HorizontalPager 翻页 + 顶/底浮动控件  
AI：完整实现 `ReaderScreen`（含 SystemBar 隐藏、点击中央显隐控件）  
用户：阅读理解 + 调试  
完成验收：从详情页章节进入阅读器，左右滑可翻页，点中央显示控制栏

#### 5.2 阅读器手势 — AI 写完整实现

目标：双击缩放、双指捏合、左右翻页、上下滚动模式切换  
AI：完整实现手势 `Modifier`（基于 `Modifier.graphicsLayer{}` + `derivedStateOf`）  
用户：在真机调试手感，必要时调整阈值  
完成验收：双击放大到 2x，再双击复原；双指捏合任意倍数；切到上下滚动模式能流畅滑

#### 5.3 Mock 图片源 + 预加载

目标：章节图片从 Mock 源加载，邻接 3 页预加载  
AI：`GetChapterImagesUseCase` + Mock + Coil3 预加载策略  
用户：实现 Coil3 ImageLoader 配置  
完成验收：进入阅读器图片快速加载；切到下一页几乎无白屏

---

### 第 6 步：收藏页（拆 3 小步）

#### 6.1 UI 骨架

目标：分组 Chip 行 + 漫画网格 + 新建分组按钮  
AI：`FavoritesScreen` 骨架  
用户：实现 ChipRow 布局  
完成验收：底部"收藏"Tab 显示分组栏 + 网格

#### 6.2 ViewModel + 分组管理

目标：新建/重命名/删除分组、移动漫画到分组（基于内存状态）  
AI：`FavoritesIntent`/`State` + ViewModel 骨架  
用户：实现交互（弹窗、长按菜单）  
完成验收：新建分组 → 切换分组 → 漫画切换；本地内存内有效

#### 6.3 SQLDelight 持久化

目标：关闭重开 App 收藏数据保留  
AI：`favorites.sq` schema + DAO 骨架 + `DriverFactory` expect/actual(Android)  
用户：实现 `FavoritesRepositoryImpl`  
完成验收：收藏 → 杀进程 → 重开 App → 数据还在

---

### 第 7 步：下载/设置/我的（拆 3 小步）

#### 7.1 三个 Tab UI 骨架

目标：下载列表占位 / 设置项列表 / 我的入口聚合  
AI：三套 Screen 骨架  
用户：实现布局  
完成验收：3 个 Tab 都能点进，UI 完整

#### 7.2 ViewModel + Mock + 持久化

目标：设置项可切换（深浅主题、阅读方向）、下载列表显示 Mock 任务  
AI：ViewModel 骨架 + `multiplatform-settings` 封装  
用户：实现交互与持久化  
完成验收：切深色 → 重启 App → 仍是深色

#### 7.3 本地漫画导入

目标："我的"里点"导入本地漫画"可选 zip 文件并显示在收藏分组里  
AI：`LocalComicImporter` 骨架（基于 Okio）  
用户：实现选文件器 expect/actual(Android)  
完成验收：选一个 zip 文件 → 收藏页"本地"分组出现新条目

---

### 第 8 步：接入真实 JS 源 [AI 兜底]（拆 4 小步）

#### 8.1 JS 引擎抽象层

目标：`expect class JsRuntime` 定义完成，Android actual 跑通 helloworld 脚本  
AI：完整实现（Android 用 Zipline 或 QuickJS-Android）  
用户：阅读理解  
完成验收：日志打印 `eval("1+1")` 结果为 `2`

#### 8.2 漫画源插件协议解析

目标：加载一个 venera 现成 JS 源（如 jmcomic.js 等示例），元数据展示在"漫画源管理"页  
AI：完整实现（对齐 venera comic_source.md）  
用户：验证加载  
完成验收："漫画源管理"页显示 JS 源元数据（名称/版本/作者）

#### 8.3 替换 Mock：真实搜索/详情/章节图片

目标：首页/详情/阅读器全部走真实 JS 源，Mock Repository 下线  
AI：Repository 改造 patch  
用户：用户对照替换  
完成验收：搜一个真实漫画名能看到结果；进详情；进阅读器看到真实漫画图

#### 8.4 下载器 + WorkManager

目标：下载页能真实下载漫画到本地，可断点续传  
AI：`DownloadScheduler` expect + Android WorkManager actual（完整实现）  
用户：实现下载列表交互  
完成验收：选一章节加入下载 → 进度条变化 → 完成后离线可读

---

### 第 9 步：iOS 铺开（拆 3 小步）

#### 9.1 iOS 端环境与构建

目标：Xcode 模拟器跑出 Hello xhunter  
AI：iosApp 配置 + cocoapods/SPM 集成指引  
用户：在 Xcode 跑通  
完成验收：iPhone 模拟器看到 "xhunter" 标题

#### 9.2 iOS 端核心模块 actual [AI 兜底]

目标：首页/详情/收藏在 iOS 模拟器全部可用  
AI：完整实现 `JsRuntime(JSC CInterop)` + `Ktor Darwin` + `SQLDelight Native Driver`  
用户：验证  
完成验收：iOS 全功能跑通

#### 9.3 iOS 阅读器手势调优

目标：iOS 上滑动/缩放手感与原生一致  
AI：调优 patch  
用户：真机调试  
完成验收：盲测分不出与 venera 原版手感差异

---

### 第 10 步：Desktop 铺开（拆 3 小步）

#### 10.1 Desktop 端窗口与构建

目标：macOS/Windows 跑出 xhunter 桌面窗口  
AI：desktopApp main + 窗口/菜单骨架  
用户：跑通 `./gradlew :desktopApp:run`  
完成验收：本机弹出桌面窗口

#### 10.2 Desktop 端核心模块 actual [AI 兜底]

目标：桌面端全部功能可用  
AI：完整实现 `JsRuntime(GraalJS/Javet)` + `Ktor CIO` + `SQLDelight JVM Driver`  
用户：验证  
完成验收：桌面端全功能跑通

#### 10.3 Desktop 打包脚本

目标：输出 .dmg / .exe 安装包  
AI：Compose Desktop packaging 配置 + Inno Setup 脚本  
用户：本机打包验证  
完成验收：双击 dmg 安装到本机能跑

---

### 第 11 步：打磨

#### 11.1 Headless 模式

目标：命令行无 UI 跑通搜索→下载流程  
AI：Headless 入口骨架  
用户：实现 CLI 参数解析  
完成验收：`./gradlew :composeApp:headless --args="search 关键词"` 输出结果

#### 11.2 EhTagTranslation 标签翻译

目标：漫画详情页标签显示中文翻译  
AI：资源加载方案  
用户：实现集成  
完成验收：英文标签 "Adventure" 自动渲染成"冒险"

#### 11.3 测试补齐 + 文档收尾

目标：关键 UseCase/ViewModel 单测覆盖；7 份文档定稿；README 含截图  
AI：Code Review + 测试模板  
用户：补充 LEARNING_NOTES + 截图  
完成验收：`./gradlew test` 全绿；docs/ 下 7 份文档完整

---

## 协作 5 环（每步都按这个走）

1. **AI 先发**：本步任务说明 + 骨架代码（含 TODO）+ 概念速查
2. **用户照做**：按文档新建文件、写实现、跑模拟器
3. **卡住找 AI**：贴报错/疑问 → AI 给关键代码片段
4. **完成 AI Review**：用户提交 → AI Code Review
5. **追加 CHANGELOG**：用户在 `docs/CHANGELOG.md` 追加一行

## 当前进度

- [x] 计划制定完成（2026-06-02）
- [x] 第 1 步：项目搭建（2026-06-02 完成）
- [ ] 第 2 步：主页框架
- [ ] 第 3 步：首页
- [ ] 第 4 步：详情页
- [ ] 第 5 步：阅读器
- [ ] 第 6 步：收藏页
- [ ] 第 7 步：下载/设置/我的
- [ ] 第 8 步：接入真实 JS 源
- [ ] 第 9 步：iOS 铺开
- [ ] 第 10 步：Desktop 铺开
- [ ] 第 11 步：打磨
