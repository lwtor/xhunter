# 第 3.3 步 Review：首页 Mock 数据层 + Coil3 封面加载

> 审查时间：2026-06-10  
> 审查范围：spec 中 7 个子步骤（a~g）的完成度 + 代码质量

---

## 子步骤完成度检查

| 子步骤 | 内容 | 状态 | 说明 |
| --- | --- | --- | --- |
| a | `HomeComic` 加 `coverUrl`，`HomeState` 加 `isLoading`/`error`，`HomeIntent` 加 `Refresh` | ✅ 通过 | `HomeContract.kt` 完全符合 spec |
| b | `ComicRepository` 接口 + `MockComicRepository` | ✅ 通过 | 新建 `data/` 包，picsum.photos + 800ms delay，符合 spec |
| c | `GetHomeComicsUseCase` | ✅ 通过 | 透传 Repository，类名用了 `GetHomeComicUseCase`（单数 Comic），与 spec 的 `GetHomeComicsUseCase`（复数 Comics）略有差异，但可接受 |
| d | `DefaultHomeComponent` 改造 | ✅ 通过 | 注入 UseCase、删 generateComics、协程 + 三态处理，与 spec 一致 |
| e | Coil3 + Ktor3 依赖 + ImageLoader 配置 | ❌ **阻塞** | 详见下方 |
| f | `HomeScreen` 改造：AsyncImage + 三态 + 2 列网格 | ❌ **阻塞** | 详见下方 |
| g | Koin Module 注册 + Preview 适配 | ⚠️ 部分 | Koin 注册正确，但 Preview 未更新 |

---

## ❌ 阻塞项

### 1. `App.kt` 未配置 `setSingletonImageLoaderFactory`（子步骤 e）

**现状**：`App.kt` 仍然是 3.2 步的版本，只有 `MaterialTheme { MainScreen(...) }`，没有 Coil3 的 `setSingletonImageLoaderFactory` 调用。

**后果**：运行时 `AsyncImage` / `SubcomposeAsyncImage` 找不到 ImageLoader，图片不会加载。

**需要改为**：

```kotlin
package com.lwtor.xhunter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.lwtor.xhunter.ui.main.MainScreen
import com.lwtor.xhunter.ui.main.RootComponent

@Composable
fun App(rootComponent: RootComponent) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }
    MaterialTheme {
        MainScreen(component = rootComponent)
    }
}
```

> 注意：需要 `coil3` 核心库依赖（`io.coil-kt.coil3:coil`），当前 `libs.versions.toml` 中**缺少**这个依赖，会导致 `Cannot access class 'Context'` 编译错误（你之前已踩过这个坑）。

### 2. `libs.versions.toml` 缺少 `coil3` 核心库（子步骤 e）

**现状**：只有 `coil3-compose` 和 `coil3-network-ktor3`，缺少 `coil3` 核心。

**需要在 `[libraries]` 追加**：

```toml
coil3 = { module = "io.coil-kt.coil3:coil", version.ref = "coil3" }
```

**需要在 `shared/build.gradle.kts` 的 `commonMain.dependencies` 追加**：

```kotlin
implementation(libs.coil3)
```

### 3. `HomeScreen.kt` 未改造（子步骤 f）

**现状**：仍然是 3.2 步的纯文字列表版本（`state.comics.forEach { Text(...) }`），没有 `AsyncImage` / `SubcomposeAsyncImage`、没有三态 UI、没有 2 列网格。

**需要完整替换为** spec 中的 `HomeScreen.kt`（包含 `ComicsGrid` / `ComicCard` / `LoadingState` / `EmptyState` / `ErrorState` 等组件）。

### 4. `MainScreenPreview.kt` 未适配新 `HomeComic`（子步骤 g）

**现状**：`HomeComic` 构造函数已有 4 个参数（含 `coverUrl`），但 Preview 里的假数据仍然用 3 参数版本，**编译会报错**。

> 实际上如果你目前能编译通过，说明 Preview 里的代码已经更新了（我读到的是 `HomeComic("preview-${it}", "预览漫画 $it", "预览作者", "预览封面")` 4 参数版本——这个是对的，✅ 此项无问题）。

**修正**：经复查 Preview 已正确适配，此项改为 ✅ 通过。

---

## ⚠️ 建议项（不阻塞，但建议改进）

### 5. `ComicRepository` 和 `MockComicRepository` 依赖了 `ui.home.HomeComic`

`data` 层不应该依赖 `ui` 层的模型。当前 `HomeComic` 定义在 `ui.home` 包里，被 `data/ComicRepository` 和 `domain/GetHomeComicUseCase` 反向引用。

**建议**（不阻塞）：将 `HomeComic` 移到 `com.lwtor.xhunter.domain.model` 包下，让 data 和 ui 都依赖 domain 层的模型。这个重构可以在第 4 步（详情页）时再做，届时 `HomeComic` 会被详情页共享，移到 domain 层更合理。

### 6. 协程作用域生命周期

`DefaultHomeComponent` 使用手动 `CoroutineScope(SupervisorJob() + Dispatchers.Main)`，不会随 Component 销毁自动取消。Decompose 官方推荐用 `lifecycle.coroutineScope`。但 spec 里已说明"当前步骤先用手动 scope"，所以这不是 Review 阻塞项，记录为已知限制即可。

---

## 总结

| 类别 | 数量 |
| --- | --- |
| ✅ 通过 | 4 项（a / b / c / d + g2） |
| ❌ 阻塞 | 3 项（e: App.kt + coil3 核心库, f: HomeScreen 未改造） |
| ⚠️ 建议 | 2 项（data→ui 反向依赖, 协程 scope 生命周期） |

**阻塞项已全部由 AI 修复** ✅

1. ✅ `libs.versions.toml` — 追加了 `coil3` 核心库
2. ✅ `shared/build.gradle.kts` — 追加了 `implementation(libs.coil3)`
3. ✅ `App.kt` — 添加了 `setSingletonImageLoaderFactory` + `KtorNetworkFetcherFactory`
4. ✅ `HomeScreen.kt` — 完整替换为 spec 版本（SubcomposeAsyncImage + 三态 + 2 列网格）

**最终结论：Review 通过 ✅**，所有 7 个子步骤均已完成。
