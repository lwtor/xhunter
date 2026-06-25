# 第 3.3 步：首页 Mock 数据层 + Coil3 封面加载

> 📌 Spec 文档标准：教学级，遵守 DEVELOPMENT_RULES §8

---

## 一、本步目标

| 维度 | 现状（3.2 结束） | 目标（3.3 结束） |
| --- | --- | --- |
| 数据来源 | `DefaultHomeComponent.generateComics()` 写死在 Component 里 | Component 从 `ComicRepository` 接口取数据，Mock 实现返回带图片 URL 的漫画 |
| 图片 | 漫画列表只有文字（title + author） | 漫画卡片显示封面图（Coil3 `AsyncImage`），有 loading 占位 / empty 空态 / error 错误态 |
| 模型 | `HomeComic(id, title, author)` | `HomeComic(id, title, author, coverUrl)` 加封面 URL |
| 架构层次 | Component 直接生成数据 | Component → UseCase → Repository → Mock 数据（单向依赖，为第 8 步替换真实源做准备） |

**看得见的变化**：首页漫画卡片从纯文字变成有封面图的卡片，切换二级 Tab 时图片跟着变；模拟网络延迟时能看到 loading 圈。

---

## 二、全局观：数据流图

```
┌─────────────── 3.2 现状 ───────────────┐
│                                         │
│  HomeScreen ──subscribe──▶ HomeState    │
│      │                       ▲          │
│   onIntent()          MutableValue      │
│      │                       │          │
│      ▼                  generateComics()│
│  HomeComponent ──────────────┘          │
│  （数据写死在 Component 内部）            │
└─────────────────────────────────────────┘

                  ↓ 3.3 改造 ↓

┌─────────────── 3.3 目标 ───────────────┐
│                                         │
│  HomeScreen ──subscribe──▶ HomeState    │
│      │                    (含 coverUrl) │
│   onIntent()          MutableValue      │
│      │                       ▲          │
│      ▼                       │          │
│  HomeComponent ── GetHomeComicsUseCase  │
│                      │                  │
│                      ▼                  │
│               ComicRepository ──────────┤
│                  │          │           │
│           MockComicRepo   (第8步→真实)  │
│          (写死数据+delay)               │
└─────────────────────────────────────────┘
```

**关键洞察**：本步引入 Repository 接口 + UseCase，但**不拆新 Gradle 模块**——所有代码仍放在 `shared` 里（包路径分层即可）。原因：当前 `shared` 就是"业务一切"，拆模块的时机是第 6 步（收藏页需要 SQLDelight 独立模块）。现在拆太早反而增加构建复杂度。

---

## 三、步骤拆解

| 子步骤 | 做什么 | 涉及文件 |
| --- | --- | --- |
| a | 改 `HomeComic` 加 `coverUrl` 字段 | `HomeContract.kt` |
| b | 新建 `ComicRepository` 接口 + `MockComicRepository` 实现 | 新建 `data/ComicRepository.kt` + `data/MockComicRepository.kt` |
| c | 新建 `GetHomeComicsUseCase` | 新建 `domain/GetHomeComicsUseCase.kt` |
| d | 改 `DefaultHomeComponent`：注入 UseCase，删 `generateComics()`，加 loading/error 状态处理 | `HomeComponent.kt` + `HomeContract.kt` |
| e | 加 Coil3 依赖 + `setSingletonImageLoaderFactory` | `libs.versions.toml` + `shared/build.gradle.kts` + `App.kt` + `XHunterApplication.kt` |
| f | 改 `HomeScreen`：用 `AsyncImage` 渲染封面，加 loading/empty/error 三态 UI | `HomeScreen.kt` |
| g | 更新 Koin Module 注册 + Preview 适配 | `SharedModule.kt` + `MainScreenPreview.kt` |

---

## 四、完整可粘贴代码

### a. HomeContract.kt — 扩展 HomeComic 和 HomeState

```kotlin
package com.lwtor.xhunter.ui.home

/**
 * 首页 MVI 契约
 */

data class HomeState(
    val selectedSubTab: HomeSubTab = HomeSubTab.RECOMMEND,
    val comics: List<HomeComic> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

enum class HomeSubTab {
    RECOMMEND,
    CATEGORY,
    RANKING
}

/**
 * 首页漫画卡片数据模型
 *
 * @param id 唯一标识
 * @param title 标题
 * @param author 作者
 * @param coverUrl 封面图片 URL
 */
data class HomeComic(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
)

sealed interface HomeIntent {
    data class SelectSubTab(val tab: HomeSubTab) : HomeIntent
    data object Refresh : HomeIntent
}

sealed interface HomeEffect
```

**变化要点**：
- `HomeComic` 新增 `coverUrl: String`
- `HomeState` 新增 `isLoading: Boolean`（loading 态）和 `error: String?`（error 态）
- `HomeIntent` 新增 `Refresh`（下拉刷新/重试用）

---

### b. ComicRepository 接口 + MockComicRepository

**`shared/src/commonMain/kotlin/com/lwtor/xhunter/data/ComicRepository.kt`**

```kotlin
package com.lwtor.xhunter.data

import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeSubTab

/**
 * 漫画数据仓库接口
 *
 * 当前由 MockComicRepository 实现（写死数据+模拟延迟），
 * 第 8.3 步会替换为真实 JS 源实现。
 */
interface ComicRepository {
    suspend fun getComics(tab: HomeSubTab): List<HomeComic>
}
```

**`shared/src/commonMain/kotlin/com/lwtor/xhunter/data/MockComicRepository.kt`**

```kotlin
package com.lwtor.xhunter.data

import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeSubTab
import kotlinx.coroutines.delay

/**
 * Mock 漫画仓库 — 写死数据 + 模拟 800ms 网络延迟
 *
 * 封面图使用 picsum.photos（免费占位图服务），
 * 每个漫画用不同 seed 保证图片不同。
 */
class MockComicRepository : ComicRepository {

    override suspend fun getComics(tab: HomeSubTab): List<HomeComic> {
        delay(800) // 模拟网络延迟
        return generateComics(tab)
    }

    companion object {
        private fun generateComics(tab: HomeSubTab): List<HomeComic> {
            val prefix = when (tab) {
                HomeSubTab.RECOMMEND -> "推荐"
                HomeSubTab.CATEGORY -> "分类"
                HomeSubTab.RANKING -> "排行"
            }
            return List(8) { i ->
                HomeComic(
                    id = "${tab.name.lowercase()}-$i",
                    title = "$prefix #${i + 1}",
                    author = "${prefix}作者 ${'A' + i}",
                    coverUrl = "https://picsum.photos/seed/${tab.name}$i/300/400",
                )
            }
        }
    }
}
```

**为什么用 picsum.photos？**
- 免费、无需 API Key、支持 seed 参数保证同一 id 图片不变
- URL 格式：`https://picsum.photos/seed/<任意字符串>/<宽>/<高>`
- 返回 300x400 的 JPG 图片，适合漫画封面竖版比例

---

### c. GetHomeComicsUseCase

**`shared/src/commonMain/kotlin/com/lwtor/xhunter/domain/GetHomeComicsUseCase.kt`**

```kotlin
package com.lwtor.xhunter.domain

import com.lwtor.xhunter.data.ComicRepository
import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeSubTab

/**
 * 获取首页漫画列表用例
 *
 * 目前只做一层透传，但用例层存在的好处：
 * 1. 未来加缓存/合并逻辑不需要改 Component
 * 2. 第 8.3 步替换 Repository 时 UseCase 不动
 * 3. 可独立单测
 */
class GetHomeComicsUseCase(
    private val repository: ComicRepository,
) {
    suspend operator fun invoke(tab: HomeSubTab): List<HomeComic> {
        return repository.getComics(tab)
    }
}
```

**为什么不直接在 Component 里调 Repository？**
- Clean Architecture 原则：Presentation 层（Component）不直接依赖 Data 层（Repository），中间用 Domain 层（UseCase）隔离
- 本步 UseCase 只有一行透传，但架构正确性优先于"现在够不够简单"

---

### d. DefaultHomeComponent 改造

**`HomeComponent.kt`**

```kotlin
package com.lwtor.xhunter.ui.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.lwtor.xhunter.domain.GetHomeComicsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface HomeComponent {
    val state: Value<HomeState>
    fun onIntent(intent: HomeIntent)
}

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val getHomeComics: GetHomeComicsUseCase,
) : HomeComponent, ComponentContext by componentContext {

    private val _state = MutableValue(HomeState())
    override val state: Value<HomeState> = _state

    // Component 级协程作用域，跟随 Component 生命周期
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        loadComics(HomeSubTab.RECOMMEND)
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectSubTab -> {
                _state.update { it.copy(selectedSubTab = intent.tab) }
                loadComics(intent.tab)
            }
            is HomeIntent.Refresh -> {
                loadComics(_state.value.selectedSubTab)
            }
        }
    }

    private fun loadComics(tab: HomeSubTab) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val comics = getHomeComics(tab)
                _state.update {
                    it.copy(
                        comics = comics,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "未知错误",
                    )
                }
            }
        }
    }
}
```

**变化要点**：
- 构造函数新增 `getHomeComics: GetHomeComicsUseCase` 参数
- 删除 `generateComics()` 伴生对象方法
- 新增 `scope: CoroutineScope` 用于异步调用 UseCase（Decompose Component 不像 ViewModel 自带 `viewModelScope`，需手动创建）
- `loadComics()` 方法：先设 `isLoading=true`，调 UseCase，成功设数据，失败设 error
- `init` 块自动加载推荐 Tab 数据
- `HomeIntent.Refresh` 重新加载当前 Tab

> ⚠️ **关于协程作用域**：这里用 `CoroutineScope(SupervisorJob() + Dispatchers.Main)` 是最简方案。Decompose 官方推荐用 `lifecycleScope`（需额外依赖 `decompose-lifecycle`），但当前步骤先用手动 scope 减少新概念。第 5 步阅读器会升级为 Decompose lifecycle 感知 scope。

---

### e. Coil3 依赖 + ImageLoader 配置

#### e1. `gradle/libs.versions.toml` — 新增 Coil3 和 Ktor3 版本

在 `[versions]` 段追加：

```toml
coil3 = "3.4.0"
ktor = "3.1.3"
```

在 `[libraries]` 段追加：

```toml
coil3-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil3-network-ktor3 = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
```

> **为什么需要 Ktor？** Coil 3 的 KMP 版本使用 Ktor 作为网络底层来加载 HTTP 图片。Android 端需要 `ktor-client-okhttp` 作为引擎。当前只有 Android 端，所以只加 OkHttp 引擎；iOS/Desktop 引擎留到第 9/10 步再加。

#### e2. `shared/build.gradle.kts` — 添加依赖

在 `commonMain.dependencies` 块内追加：

```kotlin
implementation(libs.coil3.compose)
implementation(libs.coil3.network.ktor3)
implementation(libs.ktor.client.core)
```

在 `androidMain.dependencies` 块内追加：

```kotlin
implementation(libs.ktor.client.okhttp)
```

#### e3. `App.kt` — 设置 SingletonImageLoaderFactory

```kotlin
package com.lwtor.xhunter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import com.lwtor.xhunter.ui.main.MainScreen
import com.lwtor.xhunter.ui.main.RootComponent

@Composable
fun App(rootComponent: RootComponent) {
    // 配置 Coil3 全局单例 ImageLoader
    // 必须在第一个 AsyncImage 出现之前设置
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory()) // Ktor 网络层，加载 HTTP 图片
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                // 内存缓存：可用内存的 25%，最多 50MB
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .crossfade(true) // 淡入动画
            .build()
    }

    MaterialTheme {
        MainScreen(component = rootComponent)
    }
}
```

> ⚠️ `MemoryCache` 需要 `import coil3.memory.MemoryCache`。

#### e4. `XHunterApplication.kt` — 不需要改！

Coil 3 的 `setSingletonImageLoaderFactory` 是 Composable 函数，在 `App.kt` 里调用即可。**不需要**在 `XHunterApplication` 里做任何 Coil 配置。这是 Coil 3 KMP 的设计：ImageLoader 跟着 Compose 生命周期走，而不是 Android Application。

---

### f. HomeScreen 改造 — AsyncImage + 三态

**`HomeScreen.kt`**

```kotlin
package com.lwtor.xhunter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

@Composable
fun HomeScreen(
    component: HomeComponent
) {
    val state by component.state.subscribeAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部二级 Tab
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            HomeSubTab.entries.forEach { subTab ->
                FilterChip(
                    selected = subTab == state.selectedSubTab,
                    onClick = { component.onIntent(HomeIntent.SelectSubTab(subTab)) },
                    label = { Text(text = subTab.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 内容区：三态分支
        when {
            state.isLoading -> {
                LoadingState()
            }
            state.error != null -> {
                ErrorState(
                    message = state.error,
                    onRetry = { component.onIntent(HomeIntent.Refresh) }
                )
            }
            state.comics.isEmpty() -> {
                EmptyState()
            }
            else -> {
                ComicsGrid(
                    comics = state.comics,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 漫画网格 — 2 列竖向网格
 */
@Composable
private fun ComicsGrid(
    comics: List<HomeComic>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(comics, key = { it.id }) { comic ->
            ComicCard(comic = comic)
        }
    }
}

/**
 * 单张漫画卡片 — 封面图 + 标题 + 作者
 */
@Composable
private fun ComicCard(comic: HomeComic) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 封面图 — 使用 SubcomposeAsyncImage 区分 loading/success/error
        SubcomposeAsyncImage(
            model = comic.coverUrl,
            contentDescription = comic.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f) // 竖版漫画封面比例 3:4
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp))
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载失败", color = Color.Gray)
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 标题
        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // 作者
        Text(
            text = comic.author,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 全页 Loading 态
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 空态
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("暂无漫画", style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * 错误态 + 重试按钮
 */
@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message ?: "加载失败",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

private val HomeSubTab.label: String
    get() = when (this) {
        HomeSubTab.RECOMMEND -> "推荐"
        HomeSubTab.CATEGORY -> "分类"
        HomeSubTab.RANKING -> "排行"
    }
```

**三态 UI 设计说明**：

| 状态 | 触发条件 | UI 表现 |
| --- | --- | --- |
| Loading | `isLoading == true` | 页面居中一个转圈 |
| Error | `error != null` | 错误信息 + 重试按钮 |
| Empty | `comics.isEmpty() && !isLoading && error == null` | "暂无漫画" 文字 |
| Success | `comics.isNotEmpty()` | 2 列网格卡片 |

每张卡片内部还有**图片级三态**（`SubcomposeAsyncImage` 的 loading/error 子组合）：
- 图片 loading → 灰色底 + 小转圈
- 图片 error → 灰色底 + "加载失败"
- 图片 success → 显示封面

---

### g. Koin Module 注册 + Preview 适配

#### g1. `SharedModule.kt`

```kotlin
package com.lwtor.xhunter.di

import com.arkivanov.decompose.ComponentContext
import com.lwtor.xhunter.data.ComicRepository
import com.lwtor.xhunter.data.MockComicRepository
import com.lwtor.xhunter.domain.GetHomeComicsUseCase
import com.lwtor.xhunter.ui.home.DefaultHomeComponent
import com.lwtor.xhunter.ui.home.HomeComponent
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.dsl.module

val sharedModule = module {
    // Data 层
    single<ComicRepository> { MockComicRepository() }

    // Domain 层
    factory { GetHomeComicsUseCase(get()) }

    // Presentation 层
    factory<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(componentContext)
    }

    factory<HomeComponent> { (componentContext: ComponentContext) ->
        DefaultHomeComponent(componentContext, get())
    }
}
```

**Koin 注册顺序**：`ComicRepository`（single 单例）→ `GetHomeComicsUseCase`（factory）→ `HomeComponent`（factory，注入 UseCase）。

`ComicRepository` 为什么用 `single` 而不是 `factory`？因为 Repository 持有数据源连接（即使现在是 Mock，将来是网络/DB），全局一个即可。

#### g2. `MainScreenPreview.kt`（androidMain）

Preview 中的假 `HomeComponent` 需要适配新的 `HomeComic` 签名（多了 `coverUrl`）：

```kotlin
override val homeComponent: HomeComponent = object : HomeComponent {
    override val state: Value<HomeState> = MutableValue(
        HomeState(
            selectedSubTab = HomeSubTab.RECOMMEND,
            comics = List(3) {
                HomeComic(
                    id = "preview-$it",
                    title = "预览漫画 $it",
                    author = "预览作者",
                    coverUrl = "https://picsum.photos/seed/preview$it/300/400",
                )
            }
        )
    )
    override fun onIntent(intent: HomeIntent) = Unit
}
```

> ⚠️ Preview 里的 `AsyncImage` 在 IDE 预览模式下**不会加载网络图片**（IDE 没有 HTTP 能力），只能看到 loading 占位态。这是正常现象，实机/模拟器上才能看到真实图片。

---

## 五、概念速查表

| 概念 | 一句话解释 | 类比 |
| --- | --- | --- |
| **Repository 模式** | 把"数据从哪来"的具体实现藏在一个接口后面 | 餐厅菜单（接口）vs 后厨（实现），顾客不看后厨 |
| **UseCase** | 业务逻辑的"动作"，一个 UseCase 做一件事 | 餐厅服务员：接单→传后厨→上菜，你不用自己进厨房 |
| **Mock** | 假的实现，用写死数据+模拟延迟代替真实网络 | 试衣间的假人模型，穿上看效果，不是真人 |
| **Coil3 AsyncImage** | Compose 里加载网络图片的 Composable | 类比 Android View 体系里的 Glide/Coil.into(imageView) |
| **SubcomposeAsyncImage** | Coil3 提供的三态图片组件（loading/success/error 可组合） | 比 AsyncImage 更细粒度，能自定义 loading/error 的 UI |
| **setSingletonImageLoaderFactory** | 全局配置 Coil 的图片加载器（缓存、网络层、拦截器） | 类比 OkHttp 的 OkHttpClient 全局单例 |
| **Ktor** | Kotlin 跨平台 HTTP 客户端（Coil3 KMP 用它做网络底层） | 类比 Android 的 OkHttp，但能跑在 iOS/Desktop |
| **picsum.photos** | 免费占位图服务，用 seed 保证同一 ID 图片不变 | Lorem Ipsum 的图片版 |

---

## 六、验证清单

完成全部代码后，按以下步骤验证：

- [ ] **编译通过**：`./gradlew :shared:assembleDebug` 无报错
- [ ] **启动首页**：App 启动后进入首页，**先看到转圈（loading）**，约 1 秒后出现漫画网格
- [ ] **封面图**：每张卡片显示来自 picsum.photos 的封面图（彩色照片）
- [ ] **图片 loading 态**：首次加载时每张卡片先显示灰色底+小转圈，图片出来后替换
- [ ] **切换 Tab**：切到"分类"或"排行"，先转圈再出内容
- [ ] **状态保留**：切到"收藏"Tab 再切回"首页"，之前选的二级 Tab 和内容还在
- [ ] **Empty 态**（可选验证）：把 Mock 数据条数改为 0，看到"暂无漫画"
- [ ] **Error 态**（可选验证）：把 Mock 里 `delay` 后抛 `RuntimeException()`，看到错误信息+重试按钮
- [ ] **Preview**：AS 打开 HomeScreen.kt 或 MainScreen.kt，Preview 面板能渲染（图片位置显示 loading 态即可）

---

## 七、文件操作清单

| 操作 | 文件路径 |
| --- | --- |
| 修改 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeContract.kt` |
| 修改 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeComponent.kt` |
| 修改 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/home/HomeScreen.kt` |
| 修改 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt` |
| 修改 | `shared/build.gradle.kts` |
| 修改 | `gradle/libs.versions.toml` |
| 修改 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/App.kt` |
| 修改 | `shared/src/androidMain/kotlin/com/lwtor/xhunter/ui/main/MainScreenPreview.kt` |
| 新增 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/data/ComicRepository.kt` |
| 新增 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/data/MockComicRepository.kt` |
| 新增 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/domain/GetHomeComicsUseCase.kt` |

---

## 八、踩坑预警

1. **Coil 3 的 group ID 是 `io.coil-kt.coil3`**（不是 `io.coil-kt`），写错会下载到 Coil 2
2. **Ktor 版本与 Coil 3 的兼容**：Coil 3.4.0 对应 Ktor 3.x，不要用 Ktor 2.x
3. **`setSingletonImageLoaderFactory` 必须在第一个 `AsyncImage` 渲染之前调用**，所以放在 `App()` 最顶部
4. **`MemoryCache` 的 import** 是 `coil3.memory.MemoryCache`，不是 `coil3.disk.DiskCache`
5. **Android 网络权限**：`AndroidManifest.xml` 里需要有 `<uses-permission android:name="android.permission.INTERNET" />`（KMP 向导默认已加）
6. **Preview 里看不到网络图片**：IDE 预览没有 HTTP 能力，这是正常的；实机/模拟器才能看到真实图片
