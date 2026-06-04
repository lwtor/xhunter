package com.lwtor.xhunter.ui.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.lwtor.xhunter.ui.home.HomeComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

interface RootComponent {

    val selectedTab: Value<MainTab>
    val homeComponent: HomeComponent

    fun onTabSelected(tab: MainTab)
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {
    private val _selectedTab = MutableValue(MainTab.HOME)
    override val selectedTab: Value<MainTab> = _selectedTab

    override val homeComponent: HomeComponent = get {
        parametersOf(childContext("home"))
    }

    override fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }
}