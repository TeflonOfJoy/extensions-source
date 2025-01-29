package eu.kanade.tachiyomi.extension.all.hdoujin

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class HDoujin(
    override val lang: String,
    private val HDLang: String,
) : ConfigurableSource, ParsedHttpSource() {

    override val baseUrl = "https://hdoujin.net"
    override val name = "HDoujin"
    override val supportsLatest = true
    private val json: Json by injectLazy()

    override fun headersBuilder() = super.headersBuilder()
        .set("referer", "$baseUrl/")
        .set("origin", baseUrl)

    override fun latestUpdatesRequest(page: Int) = GET(if (HDLang.isBlank()) "$baseUrl?page=$page" else "$baseUrl/browse?$HDLang&page=$page", headers)

    override fun latestUpdatesSelector() = "article.group"

    override fun latestUpdatesFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.select("a").attr("href"))
        title = element.select("a > div").text().replace("\"", "")
        thumbnail_url = element.selectFirst(".cover img")!!.let { img ->
            if (img.hasAttr("data-src")) img.attr("abs:data-src") else img.attr("abs:src")
        }
    }

    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun popularMangaSelector() = latestUpdatesSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val filterList = if (filters.isEmpty()) getFilterList() else filters
        val searchLang = HDLang.ifBlank { "" }

        val url = "$baseUrl/browse".toHttpUrl().newBuilder().addQueryParameter("lang", searchLang).addQueryParameter("s", query)

        return GET(url.build(), headers)

    }

    override fun pageListParse(document: Document): List<Page> {
        val script = document.selectFirst("script:containsData(media_server)")!!.data()
        val script2 = document.selectFirst(hentaiSelector)!!.data()

        val mediaServer = Regex("""media_server\s*:\s*(\d+)""").find(script)?.groupValues!![1]
        val json = dataRegex.find(script2)?.groupValues!![1]

        val data = json.parseAs<Hentai>()
        return data.images.pages.mapIndexed { i, image ->
            Page(
                i,
                imageUrl = "${baseUrl.replace("https://", "https://i$mediaServer.")}/galleries/${data.media_id}/${i + 1}" +
                    when (image.t) {
                        "w" -> ".webp"
                        "p" -> ".png"
                        "g" -> ".gif"
                        else -> ".jpg"
                    },
            )
        }
    }

    override fun getFilterList(): FilterList = FilterList(
        Filter.Header("Separate tags with commas (,)"),
        Filter.Header("Prepend with dash (-) to exclude"),
        TagFilter(),
        CategoryFilter(),
        CircleFilter(),
        ArtistFilter(),
        ParodyFilter(),
        CharactersFilter(),
        UploaderFilter(),
    )


}
