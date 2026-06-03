# 第 2.2.b 步开发文档：Koin DI 接入

> 阶段 A 产物 · 创建于 2026-06-03 · 节奏控制权在用户手里，AI 不主动跨阶段

## 一、本步目标（看得见什么）

完成后**界面行为与 2.2.a 完全一样**（旋转屏幕选中 Tab 不丢失），但**底层依赖装配方式换底**：

- ✅ `MainActivity` 不再自己 `new DefaultRootComponent(...)`，改成从 Koin 容器**获取**
- ✅ 启动时 logcat 看到 Koin 注入相关日志（验证容器跑通）
- ✅ `RootComponent` 通过 Koin Module 声明，未来扩展子 Component（详情/阅读器）有统一入口
- ✅ 顺手清理 TODO-2.2-1：删除 `MainScreen.kt` 第 60-73 行残留的旧 `MainScreenPreview()`

**为什么要做这步（为什么不直接 new）**：

`MainActivity` 里直接 `DefaultRootComponent(componentContext = defaultComponentContext())` 看着没问题，但真实项目里 `RootComponent` 后续会依赖：
- `Repository`（拿数据）
- `UseCase`（业务逻辑）
- `Logger`、`Settings`（基础设施）

如果继续 `new`，就要在 `MainActivity` 里把整条依赖链全 new 一遍（`DefaultRootComponent(repo = ComicRepository(api = ApiClient(...)))`），代码会膨胀。Koin 的核心价值就是**把 new 的责任从调用方转移到容器**——你只声明"谁需要谁"，容器负责装配。

本步是**搭基础设施**，所以表面上看不到大变化，但后面所有 ViewModel/UseCase/Repository 都会复用这个 Koin 容器。

## 二、核心概念速查（Koin 4.x · KMP）

### 1. Module（模块）

声明"如何创建对象"的清单。用 DSL 写：

```kotlin
val sharedModule = module {
    single<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(componentContext = componentContext)
    }
}
```

- `single` = 单例（容器里同一个实例反复用）
- `factory` = 每次调用都新建一个
- `<RootComponent>` 显式声明"按接口注册"，调用方 `get<RootComponent>()` 即可拿到 `DefaultRootComponent` 实现

### 2. 启动 Koin（startKoin）

只在 App 启动时调用一次。Android 端推荐放 `Application.onCreate()`：

```kotlin
class XhunterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()           // logcat 输出 Koin 日志
            androidContext(this@XhunterApplication)  // 让 Koin 知道 Context
            modules(sharedModule)     // 注册我们的 Module
        }
    }
}
```

### 3. 取对象（get/inject）

- **直接调** `getKoin().get<T>()` —— 同步拿，立即返回
- **委托属性** `val x: T by inject()` —— 懒拿，第一次访问时才解析（需要 `KoinComponent` 接口）

本步在 `MainActivity` 用第一种（直接 `getKoin().get`），简单直接。

### 4. 参数化注入（parameters）

`RootComponent` 的构造函数需要 `ComponentContext`，但这个 Context 是 Activity 现场创建的（`defaultComponentContext()`），**不能提前注入**。用 Koin 的 `parameters` 机制：

```kotlin
// 声明（Module 里）
single<RootComponent> { (ctx: ComponentContext) ->
    DefaultRootComponent(componentContext = ctx)
}

// 取（MainActivity 里）
val root: RootComponent = getKoin().get { parametersOf(defaultComponentContext()) }
```

`parametersOf(...)` 把运行时参数透给 Module 的 lambda。这是 Koin 处理"工厂方法+运行时入参"场景的标准做法。

### 5. 为什么不在 `commonMain` 启动 Koin

`startKoin {}` 在 Android 端要 `androidContext()`，这是平台特有的；commonMain 没法写。所以**模块定义在 commonMain，启动在 androidApp**。这正是 KMP "公共业务 + 平台粘合"分层的典型例子。

## 三、步骤拆解（三小步）

### B-1：加依赖

**修改文件**：

1. `gradle/libs.versions.toml` 追加：

   ```toml
   [versions]
   # ...
   koin = "4.0.0"

   [libraries]
   # ...
   koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
   koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
   ```

2. `shared/build.gradle.kts` 在 `commonMain.dependencies` 追加：

   ```kotlin
   implementation(libs.koin.core)
   ```

3. `androidApp/build.gradle.kts` 在 `dependencies` 追加：

   ```kotlin
   implementation(libs.koin.android)
   ```

**验收**：Gradle Sync 通过，无报错。

### B-2：写 Koin Module

**新建文件** `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt`：

```kotlin
package com.lwtor.xhunter.di

import com.arkivanov.decompose.ComponentContext
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val sharedModule = module {
    factory<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(componentContext = componentContext)
    }
}
```

**为什么用 `factory` 而不是 `single`**：
`RootComponent` 的生命周期跟 Activity 绑（每次 Activity 重建会拿到新的 `defaultComponentContext()`），用 `single` 会缓存第一个实例导致内存泄漏 + Context 失效。`factory` 每次 `get` 都新建，符合 Activity 重建语义。

> 类似 Decompose 官方示例的做法，参考：[Decompose Sample 仓库的 Koin 集成](https://github.com/arkivanov/Decompose/tree/master/sample)

### B-3：启动 Koin + 改造 MainActivity + 清理 TODO-2.2-1

#### B-3.1 新建 `XhunterApplication`

**新建文件** `androidApp/src/main/kotlin/com/lwtor/xhunter/XhunterApplication.kt`：

```kotlin
package com.lwtor.xhunter

import android.app.Application
import com.lwtor.xhunter.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class XhunterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@XhunterApplication)
            modules(sharedModule)
        }
    }
}
```

#### B-3.2 注册到 AndroidManifest

修改 `androidApp/src/main/AndroidManifest.xml`，在 `<application>` 标签加 `android:name`：

```xml
<application
    android:name=".XhunterApplication"
    android:allowBackup="true"
    ...>
```

**为什么必须改这里**：Android 启动 App 时只会实例化 manifest 里声明的 Application 类。不写 `android:name` 就用默认 `android.app.Application`，`onCreate()` 永远不被调用，Koin 永远启动不了。

#### B-3.3 改造 MainActivity 用 Koin 取根 Component

```kotlin
package com.lwtor.xhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.android.ext.android.getKoin
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Decompose 3.x：defaultComponentContext() 内部要访问 SavedStateRegistry，
        // 必须等 Activity 进入 CREATED 状态（即 super.onCreate 执行完）之后才能调。
        val componentContext = defaultComponentContext()
        val root: RootComponent = getKoin().get { parametersOf(componentContext) }

        setContent {
            App(rootComponent = root)
        }
    }
}
```

**关键点（已修正，对比 2.2.a）**：

- 2.2.a 步当时 spec 写的是「`defaultComponentContext()` 在 `super.onCreate` 之前调用」——这是**旧版 Decompose** 的写法，新版 3.x 已改。
- **Decompose 3.x 正确顺序**：`super.onCreate(savedInstanceState)` → `defaultComponentContext()` → `getKoin().get`。
- 原因：新版 `defaultComponentContext()` 会调 `SavedStateRegistry.consumeRestoredStateForKey(...)`，该方法要求 owner 已进入 `CREATED` 状态，而进入 CREATED 的时机就是 `super.onCreate` 返回时。
- 取 Component 同样放到 `super.onCreate` 之后即可——Koin 容器在 `Application.onCreate` 阶段已启动好。

> ⚠️ 如果你的 `MainActivity` 还是 2.2.a 时的旧顺序（先 `defaultComponentContext()` 再 `super.onCreate`），跑起来会抛 `IllegalStateException: You can 'consumeRestoredStateForKey' only after the corresponding component has moved to the 'CREATED' state`，详见 02-qa.md Q2。

#### B-3.4 顺手清理 TODO-2.2-1

删除 `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` 第 60-73 行残留的 `@Preview` + `private fun MainScreenPreview()`，同时删掉对应的 import：

- `androidx.compose.ui.tooling.preview.Preview`
- `com.arkivanov.decompose.value.MutableValue`

（commonMain 里这两个 import 已无用，跨模块 Preview 现在统一在 `shared/src/androidMain/.../MainScreenPreview.kt`）

## 四、文件改动清单

| 操作 | 文件 |
| --- | --- |
| 修改 | `gradle/libs.versions.toml` |
| 修改 | `shared/build.gradle.kts` |
| 修改 | `androidApp/build.gradle.kts` |
| 新建 | `shared/src/commonMain/kotlin/com/lwtor/xhunter/di/SharedModule.kt` |
| 新建 | `androidApp/src/main/kotlin/com/lwtor/xhunter/XhunterApplication.kt` |
| 修改 | `androidApp/src/main/AndroidManifest.xml` |
| 修改 | `androidApp/src/main/kotlin/com/lwtor/xhunter/MainActivity.kt` |
| 修改（清理残留） | `shared/src/commonMain/kotlin/com/lwtor/xhunter/ui/main/MainScreen.kt` |

共 **5 修改 + 2 新建**。

## 五、验收标准

### 必须满足

- [ ] **构建通过**：`./gradlew :androidApp:assembleDebug` 无报错
- [ ] **App 正常启动**：模拟器跑起来不闪退，4 Tab 切换正常
- [ ] **logcat 看到 Koin 日志**：filter `Koin`，应能看到类似 `[Koin] starting Koin ...`、`[Koin] Started 1 module(s)` 的输出（来自 `androidLogger(Level.INFO)`）
- [ ] **旋转屏幕保留 Tab**：和 2.2.a 行为一致，回归不丢
- [ ] **TODO-2.2-1 清理**：`MainScreen.kt` 不再有 `MainScreenPreview()` 残留代码

### 可选（不做也算过）

- [ ] TODO-2.2-2：`MainScreen.kt` 第 44-55 行内层 Box 嵌套扁平化为单层 `contentAlignment = Alignment.Center`

## 六、可能遇到的坑（提前打预防针）

1. **`startKoin` 重复调用**：如果开发过程中改了 Module 重新跑 App，Koin 已启动会抛 `KoinAppAlreadyStartedException`。Application 是单例，正常情况下不会重复，**热重载时**才可能触发。出问题就重启 App。

2. **`getKoin().get<RootComponent>` 找不到**：常见原因——
   - `XhunterApplication.onCreate()` 没被调用 → 检查 manifest `android:name`
   - Module 没注册到 `modules(...)` → 检查 `startKoin` 块
   - 类型不匹配 → 注意 Module 声明的是 `<RootComponent>` 接口，取的时候也用接口

3. **`parametersOf` 顺序错**：lambda 里 `(ctx: ComponentContext) ->` 要和 `parametersOf(...)` 的顺序对应。本步只有一个参数还好，多参数时容易翻车。

4. **Koin `androidLogger` 性能警告**：Release 包不要用 `Level.DEBUG/INFO`，会大量打日志。本项目 Debug 阶段用 `INFO` 即可。

## 七、不在本步范围（避免膨胀）

- ❌ 引入 ViewModel / UseCase / Repository（4.x、5.x 步骤再说）
- ❌ 在 commonMain 写 KoinComponent + by inject() 的高级用法
- ❌ iOS 端 Koin 接入（第 9 步 iOS bringup 时再做）
- ❌ Compose 内部 `koinInject()` 用法（暂时不需要）
- ❌ TODO-2.2-2 Box 扁平化（可选项，不做也行）

---

阶段 A 结束。等你说"OK"/"开始写"/"明白了"我就停在这里，开始你的编码（阶段 B）。

写代码过程中卡住直接贴报错/疑问，我会在 02-qa.md 累加记录。

完成后说 "done" / "写完了" / "提交了" / "ok 了" 触发阶段 D Review。
