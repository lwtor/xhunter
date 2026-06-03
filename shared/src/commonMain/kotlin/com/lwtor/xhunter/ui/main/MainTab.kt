package com.lwtor.xhunter.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val label: String,
    val icon: ImageVector,
) {
    Home(label = "首页", icon = Icons.Filled.Home),
    Favorites(label = "收藏", icon = Icons.Filled.Favorite),
    Explore(label = "探索", icon = Icons.Filled.Search),
    Profile(label = "个人", icon = Icons.Filled.Person),
}