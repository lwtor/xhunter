package com.lwtor.xhunter.ui.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

interface RootComponent {

    val selectedTab: Value<MainTab>

    fun onTabSelected(tab: MainTab)
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext {
    private val _selectedTab = MutableValue(MainTab.HOME)

    override val selectedTab: Value<MainTab> = _selectedTab

    override fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }
}