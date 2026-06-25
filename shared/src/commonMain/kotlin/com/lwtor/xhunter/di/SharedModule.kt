package com.lwtor.xhunter.di

import com.arkivanov.decompose.ComponentContext
import com.lwtor.xhunter.data.ComicRepository
import com.lwtor.xhunter.data.MockComicRepository
import com.lwtor.xhunter.domain.GetHomeComicUseCase
import com.lwtor.xhunter.ui.home.DefaultHomeComponent
import com.lwtor.xhunter.ui.home.HomeComponent
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.dsl.module

val sharedModule = module {

    single<ComicRepository> { MockComicRepository() }

    factory { GetHomeComicUseCase(get()) }

    factory<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(componentContext)
    }

    factory<HomeComponent> { (componentContext: ComponentContext) ->
        DefaultHomeComponent(componentContext, get())
    }
}