package com.lwtor.xhunter.ui.home

data class HomeState(
    val selectedSubTab: HomeSubTab = HomeSubTab.RECOMMEND,
    val comics: List<HomeComic> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
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
    val coverUrl: String,
)

sealed interface HomeIntent {
    data class SelectSubTab(val tab: HomeSubTab) : HomeIntent
    data object Refresh : HomeIntent
}

sealed interface HomeEffect