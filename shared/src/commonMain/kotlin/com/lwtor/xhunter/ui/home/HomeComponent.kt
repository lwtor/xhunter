package com.lwtor.xhunter.ui.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.lwtor.xhunter.domain.GetHomeComicUseCase
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
    private val getHomeComics: GetHomeComicUseCase,
) : HomeComponent, ComponentContext by componentContext {

    private val _state = MutableValue(HomeState())
    override val state: Value<HomeState> = _state

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
                        comics = comics, isLoading = false, error = null
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