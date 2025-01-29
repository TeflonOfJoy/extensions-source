package eu.kanade.tachiyomi.extension.all.hdoujin

import eu.kanade.tachiyomi.source.SourceFactory

class HDFactory : SourceFactory {
    override fun createSources() = listOf(
        HDoujin("all", ""),
        HDoujin("en", "2"),
        HDoujin("jp", "4"),
        HDoujin("zh", "8"),
        HDoujin("ko", "16"),
    )
}
