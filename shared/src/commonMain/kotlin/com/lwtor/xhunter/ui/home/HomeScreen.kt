package com.lwtor.xhunter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

@Composable
fun HomeScreen(
    component: HomeComponent
) {

    val state by component.state.subscribeAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            HomeSubTab.entries.forEach { subTab ->
                FilterChip(
                    selected = subTab == state.selectedSubTab,
                    onClick = {
                        component.onIntent(HomeIntent.SelectSubTab(subTab))
                    },
                    label = { Text(text = subTab.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        state.comics.forEach { comic ->
            Text(
                modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
                text = "${comic.title} - ${comic.author}"
            )
        }
    }
}

private val HomeSubTab.label: String
    get() = when (this) {
        HomeSubTab.RECOMMEND -> "推荐"
        HomeSubTab.CATEGORY -> "分类"
        HomeSubTab.RANKING -> "排行"
    }

@Preview
@Composable
private fun HomeScreenPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        HomeScreen(object : HomeComponent {
            override val state: Value<HomeState>
                get() = MutableValue(HomeState())

            override fun onIntent(intent: HomeIntent) {

            }
        })
    }
}