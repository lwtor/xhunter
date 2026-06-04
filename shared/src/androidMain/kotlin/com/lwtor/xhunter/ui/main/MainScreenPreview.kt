package com.lwtor.xhunter.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeComponent
import com.lwtor.xhunter.ui.home.HomeIntent
import com.lwtor.xhunter.ui.home.HomeState
import com.lwtor.xhunter.ui.home.HomeSubTab

/**
 * Preview 专用的 Fake RootComponent。
 *
 * 状态固定不变，不响应交互，仅供 IDE 渲染预览使用。
 * 放在 androidMain 而非 commonMain，因为 androidx 的 @Preview 注解
 * 仅在 Android 路径有效；同时 IDE 会自动把同模块内的 Preview 关联到
 * 被预览的 Composable（MainScreen），所以打开 MainScreen.kt 也能直接
 * 在右栏看到这些预览。
 */
private class PreviewRootComponent(
    initialTab: MainTab = MainTab.HOME,
) : RootComponent {
    override val selectedTab: Value<MainTab> = MutableValue(initialTab)
    override fun onTabSelected(tab: MainTab) = Unit

    override val homeComponent: HomeComponent = object : HomeComponent {
        override val state: Value<HomeState> = MutableValue(
            HomeState(
                selectedSubTab = HomeSubTab.RECOMMEND,
                comics = List(3) {
                    HomeComic("preview-${it}", "预览漫画 $it", "预览作者")
                }
            )
        )

        override fun onIntent(intent: HomeIntent) = Unit
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Home Tab")
@Composable
private fun MainScreenPreview_Home() {
    MainScreen(component = PreviewRootComponent(MainTab.HOME))
}

@Preview(showBackground = true, showSystemUi = true, name = "Favorites Tab")
@Composable
private fun MainScreenPreview_Favorites() {
    MainScreen(component = PreviewRootComponent(MainTab.FAVORITES))
}

@Preview(showBackground = true, showSystemUi = true, name = "Explore Tab")
@Composable
private fun MainScreenPreview_Explore() {
    MainScreen(component = PreviewRootComponent(MainTab.EXPLORE))
}

@Preview(showBackground = true, showSystemUi = true, name = "Profile Tab")
@Composable
private fun MainScreenPreview_Profile() {
    MainScreen(component = PreviewRootComponent(MainTab.CATEGORIES))
}
