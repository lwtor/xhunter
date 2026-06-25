package com.lwtor.xhunter.domain

import com.lwtor.xhunter.data.ComicRepository
import com.lwtor.xhunter.ui.home.HomeComic
import com.lwtor.xhunter.ui.home.HomeSubTab

class GetHomeComicUseCase(
    private val repository: ComicRepository
) {
    suspend operator fun invoke(tab: HomeSubTab): List<HomeComic> {
        return repository.getComics(tab)
    }
}