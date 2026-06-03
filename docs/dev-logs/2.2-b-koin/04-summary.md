# 第 2.2.b 步 总结：Koin DI 接入

> 阶段 E 产物 · 完成于 2026-06-03 · 归档目录：`docs/dev-logs/2.2-b-koin/`

---

## 一、目标回顾

把 2.2.a 步 `MainActivity.kt` 里硬编码的 `DefaultRootComponent(componentContext = ctx)` 替换成由 Koin 容器 `getKoin().get<RootComponent> { parametersOf(ctx) }` 取出，为后续大量 ViewModel/UseCase/Repository 注入打好脚手架。

附带顺手项：清理 2.2.a 遗留的 TODO-2.2-1（`MainScreen.kt` 旧 Preview）—— **本步用户决定保留，未清理**，登记为遗留项。

## 二、最终改动清单

| 操作 | 文件 | 备注 |
| --- | --- | --- |
| 修改 | `gradle/libs.versions.toml` | 新增 `koin = "4.0.0"` 版本 + `koin-core` / `koin-android` 两个 library 别名 |
| 修改 | `shared/build.gradle.kts` | `commonMain.dependencies` 追加 `implementation(libs.koin.core)` |
| 修改 | `androidApp/build.gradle.kts` | 追加 `implementation(libs.koin.android)` |
| 新建 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt` | `sharedModule = module { factory<RootComponent> { (ctx: ComponentContext) -> DefaultRootComponent(componentContext = ctx) } }` |
| 新建 | `androidApp/src/main/kotlin/com/lwtor/xhunter/XHunterApplication.kt` | `Application.onCreate` 中 `startKoin { androidLogger(Level.INFO); androidContext(this@XHunterApplication); modules(sharedModule) }` |
| 修改 | `androidApp/src/main/AndroidManifest.xml` | `<application>` 加 `android:name=".XHunterApplication"` |
| 修改 | `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt` | 顺序改为 `super.onCreate` → `defaultComponentContext()` → `getKoin().get { parametersOf(ctx) }` → `setContent` |

总计 **5 修改 + 2 新建**。lint 跨模块均 0 错误 0 警告。

## 三、看得见的功能变化

- 启动 logcat 过滤 `Koin` 关键字可看到 `[Koin] Started ... modules` 与 `Started 1 module(s)` 等日志
- Tab 切换 / 横竖屏旋转保持状态等行为与 2.2.a **完全一致**（这步是基础设施重构，用户视角无新功能）
- `MainActivity` 不再 `new DefaultRootComponent(...)`，所有 Component 都由 Koin 接管创建

## 四、踩坑记录

### 坑 1：`SharedModule.kt` 编译报红「Unresolved reference: …」

**现象**：spec B-2 步骨架代码贴进去后 import 全红
**根因**：Gradle Sync 没跑（B-1 改完依赖必须先 Sync 再写代码）
**解法**：菜单 File → Sync Project with Gradle Files；或工具栏大象图标
**详见**：`02-qa.md` Q1

### 坑 2：跑起来闪退 `IllegalStateException: You can 'consumeRestoredStateForKey' only after the corresponding component has moved to the 'CREATED' state`（spec 写错的锅）

**现象**：MainActivity onCreate 第 18 行（`val componentContext = defaultComponentContext()`）抛异常
**根因**：spec 沿用了 2.2.a 的旧版顺序「`defaultComponentContext()` 必须在 `super.onCreate` 之前调用」，但**这是 Decompose 2.x 的规则**。Decompose 3.x 改了行为：`defaultComponentContext()` 内部访问 `SavedStateRegistry.consumeRestoredStateForKey(...)`，要求 owner 已进入 `CREATED` 状态（即 `super.onCreate` 已返回）
**解法**：把顺序反过来 —— `super.onCreate(savedInstanceState)` → `defaultComponentContext()` → `getKoin().get { ... }` → `setContent`
**详见**：`02-qa.md` Q2 + `01-spec.md` 第 200-216 行已同步修正
**教训**：跨版本规则不能靠记忆，新接入库前最好查一下当前主版本的 sample。AI 文档错了第一时间登记修正 + 致歉，不替用户改业务代码。

## 五、本步采纳的关键设计决策

| 决策点 | 最终选择 | 备选 | 原因 |
| --- | --- | --- | --- |
| Module 用 `factory` 还是 `single` | **`factory`** | `single` | `RootComponent` 持有 Activity 重建后会失效的 `ComponentContext`，必须每次重新创建；`single` 会一直缓存第一次的旧 ctx |
| Module 注册类型 | **接口 `RootComponent`**（`factory<RootComponent>`） | 实现类 `DefaultRootComponent` | 调用方拿到接口，不知道实现类，符合依赖倒置；未来换实现不影响调用方 |
| Module 放哪里 | **`shared/commonMain`** | `androidApp` | iOS / Desktop 端将来可复用同一份 Module 定义 |
| 启动 Koin 放哪里 | **`androidApp` 的 `XHunterApplication.onCreate`** | `MainActivity` | Application 进程级单例；多 Activity 场景不会重复初始化 |
| `androidLogger(Level.INFO)` | **保留 INFO** | DEBUG / NONE | Debug 阶段方便 logcat 验证；上线前换 NONE 已登记到本步遗留 |

## 六、本步遗留事项

| 编号 | 项 | 文件 | 处理时机建议 |
| --- | --- | --- | --- |
| **LEGACY-2.2-b-1** | `MainScreen.kt` 第 60-73 行旧 `MainScreenPreview()` 残留（含 import `Preview` / `MutableValue` / `Value`），**用户决定保留** | `shared/src/commonMain/.../ui/main/MainScreen.kt` | 等 androidMain 那份 Preview 用顺手了再删；不阻塞 |
| **TODO-2.2-b-1** | `MainActivity.kt` 第 15-16 行注释还是旧版「必须在 super.onCreate **之前**调用」，与实际代码顺序矛盾 | `androidApp/.../MainActivity.kt` | 下一步顺手改成「Decompose 3.x：必须在 super.onCreate 之后调用」 |
| **TODO-2.2-b-2** | `XHunterApplication` 类名大小写未对齐 spec（spec=小 h `Xhunter`，实际=大 H `XHunter`） | `androidApp/.../XHunterApplication.kt` + `AndroidManifest.xml` | 用户基线决定权；建议改成 `XhunterApplication` 与产品名锚点 `xhunter` 一致；改动 4 处（文件名 / 类名 / Manifest / `this@`） |
| TODO-2.2-b-3 | `XHunterApplication` 内部空行风格 | 同上 | 个人偏好，不动 |
| TODO-2.2-2（继承自 2.2.a） | `MainScreen.kt` 内层 Box 嵌套可扁平化 | `shared/.../ui/main/MainScreen.kt` | 任意小步骤顺手处理 |

> **TODO-2.2-1（清理旧 Preview）已在本步不处理**，转记为 LEGACY-2.2-b-1。

## 七、知识沉淀（可补充进 LEARNING_NOTES.md）

### Koin 4.x 心智模型

- **Module = 配方清单**：`module { ... }` 块声明「类型 X 怎么造」
- **`single<X> { ... }`** = 全局单例（造一次，所有调用方共用）
- **`factory<X> { ... }`** = 每次调用都现造一份（适合带运行时参数的对象）
- **`get<X>()`** = 顾客来取菜（懒加载，第一次取才造）
- **`get<X> { parametersOf(arg1, arg2) }`** = 带运行时参数取菜（factory 接收方写 `factory<X> { (a: A, b: B) -> ... }`）

### KMP 项目典型 DI 分层

```
shared/commonMain    → 模块定义（与平台无关的 Repository / UseCase / 跨平台 Component）
shared/androidMain   → Android 平台 actual 实现的注册（如 SQLDelight Driver）
androidApp/Application → 启动 Koin、注册 Android 上下文（androidContext / androidLogger）
```

### Decompose 3.x 与旧版的关键差异

`defaultComponentContext()` 在新版必须在 `super.onCreate(savedInstanceState)` **之后**调用，旧版（≤ 2.x）是「之前」。**新接入库前先查当前版本 sample**。

## 八、Git 状态预览

> 本步暂未触发 commit；用户后续说 `commit` 时再走 ydb 约定的两步预览流程。

涉及文件（待提交）：
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `androidApp/build.gradle.kts`
- `androidApp/src/main/AndroidManifest.xml`
- `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt`
- `androidApp/src/main/kotlin/com/lwtor/xhunter/XHunterApplication.kt`（新建）
- `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt`（新建）
- `docs/dev-logs/2.2-b-koin/01-spec.md` ~ `04-summary.md`（开发文档归档）
- `docs/CHANGELOG.md`（追加 2.2.b 段）
- `docs/ROADMAP.md`（2.2.b 状态打勾）

## 九、下一步

**第 2.3 步：文档回填** — `docs/ARCHITECTURE.md` + `docs/MODULES.md`，含模块依赖 Mermaid 图。
此步骨架以文档生成为主，AI 出草稿，用户检查补充，无业务代码改动，节奏会比 2.2.b 更轻量。

> ⏸ AI 在此停下，等用户说「下一步」/「开始第 2.3 步」/「继续」再进入第 2.3 步阶段 A。
