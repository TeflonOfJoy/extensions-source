package eu.kanade.tachiyomi.extension.en.godamanga

import org.jsoup.Jsoup
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class GoDamanga : ParsedHttpSource() {

    override val name = "GoDamanga"

    override val baseUrl = "https://godamanga.art/"

    override val lang = "en"

    override val supportsLatest = true


    //popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/hots?page=${page}", headers)
    }

    override fun popularMangaSelector() = ".entries article"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.title = element.select("entry-title a").text()
        manga.url = element.selectFirst("entry-title a")!!.attr("href")
        manga.thumbnail_url = element.select("ct-image-container img").attr("src")

        return manga
    }

    override fun popularMangaNextPageSelector() = ".ct-pagination .next"

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/newss?page=${page}", headers)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isEmpty()) {
            var url = baseUrl.toHttpUrl().newBuilder()
            filters.forEach {
                filter -> when (filter) {
                    is GenreFilter -> filter.toUriPart().let {
                        url.apply {
                            addPathSegment("page")
                            addPathSegment(page.toString())
                            addQueryParameter("s", query)
                        }
                    } else -> {}
                }
            }
            GET(url.build(), headers)
        } else {
            GET("$baseUrl/page/$page?s=$query", headers)
        }

    }
    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Separator(),
        GenreFilter(),
        )

    private class GenreFilter : UriPartFilter(
        "Category",
        arrayOf(
            Pair("All", "allmanga"),
            Pair("Manga", "1057"),
            Pair("Manhua", "1010"),
            Pair("Manhwa", "996"),
        )
    )

    private open class UriPartFilter(displayName: String, private val vals: Array<Pair<String, String>>) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.author = document.selectFirst("div.author-content")?.text()
        manga.genre = document.selectFirst("div.genres-content a")?.text()
        manga.description = document.select(".stk-block-text__text").text()
        manga.thumbnail_url = document.select("figure img").attr("src")

        val statusText = document.select("div.author-content:contains(Status)").text()
        manga.status = when {
            statusText.contains("OnGoing", true) -> SManga.ONGOING
            statusText.contains("Complete", true) -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        return manga
    }

    override fun chapterListRequest(manga: SManga) = chapterListRequest(manga.url, 1)

    private fun extractLastPathFromUrl(url: String): String? {
        val httpUrl = url.toHttpUrl()
        return httpUrl.pathSegments.lastOrNull()
    }

    private fun chapterListRequest(url: String, page: Int): Request {
        val mangaName = extractLastPathFromUrl(url)
        return GET("$baseUrl/chapterlist/$mangaName.?page=${page}", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = document.select(chapterListSelector()).map(::chapterFromElement).toMutableList()
        return chapters
    }

    override fun chapterListSelector() = ".version-chaps"

    private val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH)


    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        chapter.url = element.select("a").attr("href")
        chapter.name = element.select("a").text()
        chapter.date_upload = try {
            element.select("a span.chapter-release-date").text().let {
                dateFormat.parse(it)?.time ?: 0L
            } ?: 0L
        } catch (_: ParseException) {
            0L
        }

        return chapter
    }



}

