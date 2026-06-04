package com.lwtor.xhunter.ui.home

data class HomeState(
    val selectedSubTab: HomeSubTab = HomeSubTab.RECOMMEND,
    val comics: List<HomeComic> = emptyList(),
)

enum class HomeSubTab {
    RECOMMEND,
    CATEGORY,
    RANKING
}

/**
 * HomeComic
 */
data class HomeComic(
    val id: String,
    val title: String,
    val author: String,
)

sealed interface HomeIntent {
    data class SelectSubTab(val tab: HomeSubTab) : HomeIntent
}

sealed interface HomeEffect