package eu.kanade.tachiyomi.extension.bn.bntoon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class BnToon : HttpSource() {
    override val name = "BnToon"
    override val baseUrl = "https://bntoon.com"
    override val lang = "bn"
    override val supportsLatest = true

    override fun headersBuilder() = super.headersBuilder().add("Referer", baseUrl)

    // Popular Manga
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/?page=$page", headers)
    }

    override fun popularMangaParse(response: okhttp3.Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(".bntoon-series-card, .bntoon-popular-item").map { element ->
            popularMangaFromElement(element)
        }
        return MangasPage(mangas, true)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        val link = element.selectFirst("a[href]")
        manga.url = link?.attr("href") ?: ""
        manga.title = element.selectFirst("h3, h4, .bntoon-series-card-title, .bntoon-popular-item-title")?.text() ?: ""
        manga.thumbnail_url = element.selectFirst("img")?.absUrl("src") ?: ""
        return manga
    }

    // Latest Updates
    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)
    override fun latestUpdatesParse(response: okhttp3.Response): MangasPage = popularMangaParse(response)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/?s=$query&page=$page", headers)
    }

    override fun searchMangaParse(response: okhttp3.Response): MangasPage = popularMangaParse(response)

    // Manga Details
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.selectFirst("h1.bntoon-series-title, h1")?.text() ?: ""
        manga.thumbnail_url = document.selectFirst(".bntoon-poster-image, img[src]")?.absUrl("src") ?: ""
        manga.author = document.selectFirst(".bntoon-synopsis-pill:contains(Author)")?.text()?.replace("Author:", "")?.trim() ?: ""
        manga.artist = document.selectFirst(".bntoon-synopsis-pill:contains(Artist)")?.text()?.replace("Artist:", "")?.trim() ?: ""
        manga.genre = document.select(".bntoon-genre-tag").joinToString(", ") { it.text() }
        manga.description = document.selectFirst(".bntoon-synopsis-text, .bntoon-series-synopsis")?.text() ?: ""
        return manga
    }

    // Chapter List
    override fun chapterListParse(response: okhttp3.Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(".bntoon-chapter-item, .reader-chapterlist-item").map { element ->
            val chapter = SChapter.create()
            chapter.url = element.selectFirst("a[href]")?.attr("href") ?: ""
            chapter.name = element.selectFirst(".bntoon-ch-number, .reader-chapterlist-num")?.text() ?: ""
            chapter
        }.reversed()
    }

    // Pages
    override fun pageListParse(response: okhttp3.Response): List<Page> {
        val document = response.asJsoup()
        return document.select(".reader-page img.reader-image, .reader-image").mapIndexed { index, element ->
            Page(index, "", element.absUrl("src"))
        }
    }

    override fun imageUrlParse(document: Document): String {
        return document.selectFirst("img")?.absUrl("src") ?: ""
    }
}
