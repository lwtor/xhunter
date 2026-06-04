package com.lwtor.xhunter.ui.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update

interface HomeComponent {
    val state: Value<HomeState>
    fun onIntent(intent: HomeIntent)
}

class DefaultHomeComponent(
    componentContext: ComponentContext,
) : HomeComponent, ComponentContext by componentContext {

    private val _state = MutableValue(
        HomeState(
            selectedSubTab = HomeSubTab.RECOMMEND,
            comics = generateComics(HomeSubTab.RECOMMEND),
        )
    )

    override val state: Value<HomeState> = _state

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectSubTab -> {
                _state.update {
                    it.copy(
                        selectedSubTab = intent.tab,
                        comics = generateComics(intent.tab),
                    )
                }
            }
        }
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
                )
            }
        }
    }
}