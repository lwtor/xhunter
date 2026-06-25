package com.lwtor.xhunter.data

import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeSubTab

interface ComicRepository {
    suspend fun getComics(tab: HomeSubTab): List<HomeComic>
}