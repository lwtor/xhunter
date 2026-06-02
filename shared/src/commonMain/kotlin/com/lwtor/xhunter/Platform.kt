package com.lwtor.xhunter

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform