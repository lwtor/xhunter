package com.lwtor.xhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 注意：defaultComponentContext() 必须在 super.onCreate(savedInstanceState) 之前调用，
        // 它内部要接管 savedInstanceState 完成 StateKeeper / InstanceKeeper 还原。
        val root: RootComponent = DefaultRootComponent(
            componentContext = defaultComponentContext(),
        )
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            App(rootComponent = root)
        }
    }
}
