package com.lwtor.xhunter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.ImageLoader.Builder
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.lwtor.xhunter.ui.main.MainScreen
import com.lwtor.xhunter.ui.main.RootComponent

@Composable
fun App(rootComponent: RootComponent) {
    setSingletonImageLoaderFactory { context ->
        Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }

    MaterialTheme {
        MainScreen(component = rootComponent)
    }
}
