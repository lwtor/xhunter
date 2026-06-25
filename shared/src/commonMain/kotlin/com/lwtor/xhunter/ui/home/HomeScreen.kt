package com.lwtor.xhunter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
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
        // 顶部二级 Tab
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            HomeSubTab.entries.forEach { subTab ->
                FilterChip(
                    selected = subTab == state.selectedSubTab,
                    onClick = { component.onIntent(HomeIntent.SelectSubTab(subTab)) },
                    label = { Text(text = subTab.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 内容区：三态分支
        when {
            state.isLoading -> {
                LoadingState()
            }
            state.error != null -> {
                ErrorState(
                    message = state.error,
                    onRetry = { component.onIntent(HomeIntent.Refresh) }
                )
            }
            state.comics.isEmpty() -> {
                EmptyState()
            }
            else -> {
                ComicsGrid(
                    comics = state.comics,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 漫画网格 — 2 列竖向网格
 */
@Composable
private fun ComicsGrid(
    comics: List<HomeComic>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(comics, key = { it.id }) { comic ->
            ComicCard(comic = comic)
        }
    }
}

/**
 * 单张漫画卡片 — 封面图 + 标题 + 作者
 */
@Composable
private fun ComicCard(comic: HomeComic) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 封面图
        SubcomposeAsyncImage(
            model = comic.coverUrl,
            contentDescription = comic.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp))
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载失败", color = Color.Gray)
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = comic.author,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("暂无漫画", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message ?: "加载失败",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

private val HomeSubTab.label: String
    get() = when (this) {
        HomeSubTab.RECOMMEND -> "推荐"
        HomeSubTab.CATEGORY -> "分类"
        HomeSubTab.RANKING -> "排行"
    }
