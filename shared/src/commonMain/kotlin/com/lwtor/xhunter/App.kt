package com.lwtor.xhunter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.lwtor.xhunter.ui.main.MainScreen
import com.lwtor.xhunter.ui.main.RootComponent

@Composable
fun App(rootComponent: RootComponent) {
    MaterialTheme {
        MainScreen(component = rootComponent)
    }
}
