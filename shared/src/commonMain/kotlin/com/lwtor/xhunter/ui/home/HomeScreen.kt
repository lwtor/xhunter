package com.lwtor.xhunter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        placeholderComic.forEach { comic ->
            Text(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                text = "${comic.title} - ${comic.author}"
            )
        }
    }
}

private data class PlaceholderComic(
    val id: String,
    val title: String,
    val author: String,
)

private val placeholderComic = List(8) { i ->
    PlaceholderComic(
        id = "demo-$i", title = "示例漫画 ${i + 1}", author = "作者 ${'A' + i}"
    )
}

@Preview
@Composable
private fun HomeScreenPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        HomeScreen()
    }
}