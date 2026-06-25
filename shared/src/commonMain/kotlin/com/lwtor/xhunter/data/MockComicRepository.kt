package com.lwtor.xhunter.data

import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeSubTab
import kotlinx.coroutines.delay

class MockComicRepository : ComicRepository {
    override suspend fun getComics(tab: HomeSubTab): List<HomeComic> {
        delay(800)
        return generateComics(tab)
    }

    companion object {
        private fun generateComics(tab: HomeSubTab): List<HomeComic> {
            val prefix = when (tab) {
                HomeSubTab.RECOMMEND -> "推荐"
                HomeSubTab.CATEGORY -> "分类"
                HomeSubTab.RANKING -> "排行"
            }
            return List(8) { i ->
                HomeComic(
                    id = "${tab.name.lowercase()}-$i",
                    title = "$prefix #${i + 1}",
                    author = "${prefix}作者 ${'A' + i}",
                    coverUrl = "https://picsum.photos/seed/${tab.name}$i/300/400",
                )
            }
        }
    }
}