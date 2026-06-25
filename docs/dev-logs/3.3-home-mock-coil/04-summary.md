# 第 3.3 步总结：首页 Mock 数据层 + Coil3 封面加载

> 归档时间：2026-06-10

---

## 一、本步做了什么

首页从"写死数据+纯文字"升级为 **Repository + UseCase 架构 + Coil3 封面图 + loading/empty/error 三态**。

### 核心变化

| 维度 | 3.2 结束 | 3.3 结束 |
| --- | --- | --- |
| 数据来源 | `DefaultHomeComponent.generateComics()` 写死 | Component → UseCase → Repository → Mock 数据 |
| 图片 | 纯文字列表 | `SubcomposeAsyncImage` 封面 + 图片级三态 |
| 架构层次 | Presentation 直出数据 | Presentation → Domain(UseCase) → Data(Repository) |
| 全页状态 | 无 | loading / empty / error / success 四态 |

### 子步骤完成情况

| 子步骤 | 内容 | 完成方式 |
| --- | --- | --- |
| a | `HomeComic` 加 `coverUrl`，`HomeState` 加 `isLoading`/`error`，`HomeIntent` 加 `Refresh` | 用户实现 |
| b | `ComicRepository` 接口 + `MockComicRepository` | 用户实现 |
| c | `GetHomeComicUseCase` | 用户实现 |
| d | `DefaultHomeComponent` 改造 | 用户实现 |
| e | Coil3 + Ktor3 依赖 + `setSingletonImageLoaderFactory` | 用户实现 + AI 修复（补充 `coil3` 核心库依赖） |
| f | `HomeScreen` 改造：AsyncImage + 三态 + 2 列网格 | AI 修复（用户未改造，AI 补全） |
| g | Koin Module 注册 + Preview 适配 | 用户实现 |

---

## 二、Review 中发现的问题及处理

### 阻塞项（已修复）

1. **`libs.versions.toml` 缺少 `coil3` 核心库** — 导致 `Cannot access class 'Context'` 编译错误
   - 修复：追加 `coil3 = { module = "io.coil-kt.coil3:coil", version.ref = "coil3" }`
2. **`shared/build.gradle.kts` 缺少 `implementation(libs.coil3)`** — commonMain 无法访问 Coil 3 核心类型
   - 修复：追加依赖
3. **`App.kt` 未配置 `setSingletonImageLoaderFactory`** — 运行时 AsyncImage 找不到 ImageLoader
   - 修复：添加完整 Coil3 配置（KtorNetworkFetcherFactory + crossfade）
4. **`HomeScreen.kt` 未改造** — 仍是 3.2 步纯文字版本
   - 修复：完整替换为 spec 版本（SubcomposeAsyncImage + ComicsGrid + 三态组件）

### 建议项（不阻塞，留后续步骤处理）

- **SUGGEST-3.3-1**：`HomeComic` 目前在 `ui.home` 包，`data`/`domain` 层反向引用了它 → 第 4 步时移到 `domain.model`
- **SUGGEST-3.3-2**：`DefaultHomeComponent` 手动 `CoroutineScope` 不会随 Component 销毁自动取消 → 第 5 步升级为 Decompose lifecycle 感知 scope

---

## 三、新增依赖

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| `io.coil-kt.coil3:coil` | 3.4.0 | Coil3 核心（PlatformContext 等基础类型） |
| `io.coil-kt.coil3:coil-compose` | 3.4.0 | Compose AsyncImage 组件 |
| `io.coil-kt.coil3:coil-network-ktor3` | 3.4.0 | Ktor3 网络层 fetcher |
| `io.ktor:ktor-client-core` | 3.1.3 | Ktor 核心客户端 |
| `io.ktor:ktor-client-okhttp` | 3.1.3 | Ktor Android OkHttp 引擎 |

---

## 四、关键踩坑

1. **Coil 3 的 `setSingletonImageLoaderFactory` 需要 `coil3` 核心库**：只加 `coil-compose` 不加 `coil` 会导致 `Cannot access class 'Context'` 编译错误。`PlatformContext` 类型定义在 `coil` 核心模块里。
2. **`setSingletonImageLoaderFactory` 是 Composable**：必须在第一个 AsyncImage 渲染之前调用，放在 `App()` 顶部最安全。
3. **Coil 3 group ID 是 `io.coil-kt.coil3`**（不是 `io.coil-kt`），写错会下到 Coil 2。

---

## 五、遗留项

- LEGACY-3.3-1：`HomeComic` 定义在 `ui.home` 包导致 data/domain 层反向引用 ui 层（SUGGEST-3.3-1，留第 4 步处理）
- LEGACY-3.3-2：`DefaultHomeComponent` 协程 scope 不跟随 Component 生命周期（SUGGEST-3.3-2，留第 5 步升级）
