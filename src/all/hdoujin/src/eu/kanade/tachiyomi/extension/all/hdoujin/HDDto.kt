package eu.kanade.tachiyomi.extension.all.nhentai

import kotlinx.serialization.Serializable

@Serializable
class HDDto(
    var id: Int,
    val images: Images,
    val tags: List<Tag>,
    val title: Title,
    val upload_date: Long,
    val num_favorites: Long,
)

@Serializable
class Title(
    var english: String? = null,
    val japanese: String? = null,
)

@Serializable
class Images(
    val pages: List<Image>,
)

@Serializable
class Image(
    val t: String,
)

@Serializable
class Tag(
    val name: String,
)
