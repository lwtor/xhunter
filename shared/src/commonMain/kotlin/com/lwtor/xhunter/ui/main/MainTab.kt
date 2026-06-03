package com.lwtor.xhunter.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val label: String,
    val icon: ImageVector,
) {
    HOME(label = "主页", icon = Icons.Filled.Home),
    FAVORITES(label = "收藏", icon = Icons.Filled.Favorite),
    EXPLORE(label = "探索", icon = Icons.Filled.Search),
    CATEGORIES(label = "分类", icon = Icons.Filled.Menu),
}