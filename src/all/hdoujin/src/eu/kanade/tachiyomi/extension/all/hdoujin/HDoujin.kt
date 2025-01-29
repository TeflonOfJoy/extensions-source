package eu.kanade.tachiyomi.extension.all.hdoujin

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class HDoujin(
    override val lang: String,
    private val HDLang: String,
) : ConfigurableSource, ParsedHttpSource() {

    override val baseUrl = "https://hdoujin.net"
    override val name = "HDoujin"
    override val supportsLatest = true
    override fun chapterFromElement(element: Element): SChapter {
        TODO("Not yet implemented")
    }

    override fun chapterListSelector(): String {
        TODO("Not yet implemented")
    }

    private val json: Json by injectLazy()

    override fun headersBuilder() = super.headersBuilder()
        .set("referer", "$baseUrl/")
        .set("origin", baseUrl)

    override fun imageUrlParse(document: Document): String {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesRequest(page: Int) = GET(if (HDLang.isBlank()) "$baseUrl?page=$page" else "$baseUrl/browse?$HDLang&page=$page", headers)

    override fun latestUpdatesSelector() = "article.group"
    override fun mangaDetailsParse(document: Document): SManga {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.select("a").attr("href"))
        title = element.select("a > div").text().replace("\"", "")
        thumbnail_url = element.selectFirst(".cover img")!!.let { img ->
            if (img.hasAttr("data-src")) img.attr("abs:data-src") else img.attr("abs:src")
        }
    }

    override fun latestUpdatesNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun popularMangaNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/popular?page=$page", headers)

    override fun popularMangaSelector() = latestUpdatesSelector()
    override fun searchMangaFromElement(element: Element): SManga {
        TODO("Not yet implemented")
    }

    override fun searchMangaNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val mangaUrl = response.request.url
        val page = response.request.url.queryParameter("page")!!.toInt() + 1
        val nextPageUrl = mangaUrl.newBuilder().setQueryParameter("page", page.toString()).build()
        val nextPageDocument = client.newCall(GET(nextPageUrl, headers)).execute().asJsoup()
        var hasNextPage = true
        if (nextPageDocument.select(popularMangaSelector()).isEmpty()) {
            hasNextPage = false
        }
        val document = response.asJsoup()
        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        return MangasPage(mangas, hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val filterList = if (filters.isEmpty()) getFilterList() else filters
        val searchLang = HDLang.ifBlank { "" }

        val url = "$baseUrl/browse".toHttpUrl().newBuilder().addQueryParameter("lang", searchLang).addQueryParameter("s", query)

        return GET(url.build(), headers)
    }

    override fun searchMangaSelector(): String {
        TODO("Not yet implemented")
    }

    override fun pageListParse(document: Document): List<Page> {
        TODO()
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        TODO("Not yet implemented")
    }
}
