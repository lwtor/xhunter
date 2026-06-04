# 第 3.2 步 归档总结

> 阶段 E 产出。

---

## 一、本步做了什么

把首页二级 Tab 的"切换 → 内容变化"从临时 `mutableStateOf` 升级成 **MVI（Intent/State/Effect）+ Decompose Component** 驱动。列表数据仍写死在 `generateComics()` 里，但通过 `HomeState.comics` 暴露，为 3.3 步接入 Repository 预留了接口。

核心链路：
```
用户点 Tab → HomeScreen 调 component.onIntent(SelectSubTab)
→ DefaultHomeComponent 更新 MutableValue<HomeState>
→ subscribeAsState() 触发重组 → UI 显示新数据
```

---

## 二、新增/修改文件清单

| 文件 | 操作 | 说明 |
| --- | --- | --- |
| `ui/home/HomeContract.kt` | 🆕 | MVI 契约（State/Intent/Effect/SubTab/Comic） |
| `ui/home/HomeComponent.kt` | 🆕 | 接口 + 默认实现（状态权威） |
| `di/SharedModule.kt` | ✏️ | 追加 `factory<HomeComponent>` |
| `ui/main/RootComponent.kt` | ✏️ | 新增 `homeComponent` 属性 + `KoinComponent` + `childContext` |
| `ui/main/MainScreen.kt` | ✏️ | 透传 `component.homeComponent` 给 HomeScreen |
| `ui/home/HomeScreen.kt` | ✏️ | 改签名，从 Component 读状态/发意图 |
| `ui/main/MainScreenPreview.kt` | ✏️ | Preview 桩适配 `homeComponent` |

---

## 三、关键技术决策

| 决策 | 选择 | 原因 |
| --- | --- | --- |
| 状态容器 | `MutableValue<Value<T>>` | Decompose 原生，可脱离 Compose 单测，与 `StateFlow` 范式一致 |
| Intent 形式 | `sealed interface` | 编译器强制穷举 when，新增意图不会漏 |
| 子 Component 创建 | `childContext("home")` + Koin `get { parametersOf(...) }` | 保持与 RootComponent 同模式，生命周期跟随父 |
| 数据生成位置 | `companion object` 内 `generateComics()` | 3.3 步替换为 Repository 时只需改 onIntent 内部，不动签名 |

---

## 四、与 spec 的偏差

无偏差。用户完全按教学级 spec 的 a~g 子步骤实现，7 个文件均与 spec 一致。

---

## 五、遗留项

| ID | 描述 | 优先级 | 建议 |
| --- | --- | --- | --- |
| LEGACY-3.2-1 | `RootComponent.kt:11` 误引 `import kotlin.coroutines.EmptyCoroutineContext.get` | 低 | 下次改 RootComponent 时顺手删 |
| LEGACY-3.2-2 | `MainScreen.kt` commonMain `@Preview` 与 androidMain 重复 | 低 | 下次清理 Preview 时统一删 |
| LEGACY-3.2-3 | `HomeScreen.kt` commonMain `@Preview` 同上 | 低 | 同上 |
| LEGACY-3.2-4 | `HomeContract.kt` HomeComic KDoc 过简 | 低 | 下次改 HomeContract 时补 |

---

## 六、3.3 步衔接

3.3 步（首页 Mock 数据层 + Coil3 封面）需要：
1. `HomeState` 加 `loadState: LoadState`（Idle/Loading/Success/Error），UI 三态
2. `HomeIntent` 加 `LoadInitial` / `Retry`
3. `DefaultHomeComponent.onIntent(SelectSubTab)` 里"直接 `generateComics()`"改为调用 `Repository`
4. 新建 `MockComicRepository` + `GetHomeListUseCase`
5. `HomeComic` 加 `coverUrl: String`，卡片用 Coil3 `AsyncImage`
6. `HomeScreen` 加 loading/empty/error 三态 UI 分支

本步 `HomeState.comics: List<HomeComic>` 和 `HomeIntent.SelectSubTab` 已预留好，3.3 步在外面包一层即可，不需要改契约核心。

---

## 七、协作规则变更

本步在 `docs/DEVELOPMENT_RULES.md` §8 新增了「Spec 文档标准（教学级，强制）」子章节，6 条规则：

1. 全局观优先（现状 vs 目标图 + 数据流图）
2. 子步骤完整可操作（完整代码 + 解释 + 验证）
3. 修改型子步骤展示"当前 → 改成"
4. 概念速查表（类比 Android 已知概念）
5. 验证检查清单（编译 + 运行 + 自查）
6. 不做的事 + 下步预告

后续所有步骤的 spec 必须按此标准输出。
