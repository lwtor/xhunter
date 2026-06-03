package com.lwtor.xhunter.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

@Composable
fun MainScreen(
    component: RootComponent,
) {

    val selected by component.selectedTab.subscribeAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = {
                            component.onTabSelected(tab)
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(text = tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().align(Alignment.Center)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "当前 Tab: ${selected.label}"
                )
            }
        }
    }

}

@Preview
@Composable
private fun MainScreenPreview() {
    MainScreen(
        component = object : RootComponent {
            override val selectedTab: Value<MainTab>
                get() = MutableValue(MainTab.HOME)

            override fun onTabSelected(tab: MainTab) {

            }
        },
    )
}