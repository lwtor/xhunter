package com.lwtor.xhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.lwtor.xhunter.ui.main.DefaultRootComponent
import com.lwtor.xhunter.ui.main.RootComponent
import org.koin.android.ext.android.getKoin
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 注意：defaultComponentContext() 必须在 super.onCreate(savedInstanceState) 之前调用，
        // 它内部要接管 savedInstanceState 完成 StateKeeper / InstanceKeeper 还原。

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val componentContext = defaultComponentContext()
        val root: RootComponent = getKoin().get { parametersOf(componentContext) }

        setContent {
            App(rootComponent = root)
        }
    }
}