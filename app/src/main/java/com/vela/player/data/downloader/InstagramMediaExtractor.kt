package com.vela.player.data.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class ExtractedInstagramImages(
    val title: String,
    val author: String?,
    val imageUrls: List<String>,
    val requiresLogin: Boolean = false
)

/**
 * Lightweight on-device extractor for Instagram photo posts, carousels, and photo stories.
 * Complements yt-dlp, which focuses strictly on video and audio formats.
 */
class InstagramMediaExtractor(
    private val cookieStore: InstagramCookieStore
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun extractImages(rawUrl: String): ExtractedInstagramImages = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.substringBefore("?").trimEnd('/')
        val cookieHeader = cookieStore.getCookieHeaderForHttp()

        // 1. Try Instagram JSON endpoint with session cookies if available
        val jsonAttempt = runCatching {
            val jsonUrl = "$cleanUrl/?__a=1&__d=dis"
            val req = buildRequest(jsonUrl, cookieHeader, acceptJson = true)
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403 || resp.request.url.encodedPath.contains("/accounts/login")) {
                    return@runCatching ExtractedInstagramImages(
                        title = "Instagram Post",
                        author = null,
                        imageUrls = emptyList(),
                        requiresLogin = true
                    )
                }
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful && body.trimStart().startsWith("{")) {
                    parseInstagramJson(body)
                } else {
                    null
                }
            }
        }.getOrNull()

        if (jsonAttempt != null && (jsonAttempt.imageUrls.isNotEmpty() || jsonAttempt.requiresLogin)) {
            return@withContext jsonAttempt
        }

        // 2. Fallback: fetch page HTML and parse embedded JSON / OpenGraph og:image
        val htmlAttempt = runCatching {
            val req = buildRequest("$cleanUrl/", cookieHeader, acceptJson = false)
            client.newCall(req).execute().use { resp ->
                val finalPath = resp.request.url.encodedPath
                if (finalPath.contains("/accounts/login")) {
                    return@runCatching ExtractedInstagramImages(
                        title = "Instagram Post",
                        author = null,
                        imageUrls = emptyList(),
                        requiresLogin = true
                    )
                }
                val html = resp.body?.string().orEmpty()
                parseInstagramHtml(html)
            }
        }.getOrNull()

        htmlAttempt ?: ExtractedInstagramImages(
            title = "Instagram Media",
            author = null,
            imageUrls = emptyList(),
            requiresLogin = cookieHeader.isNullOrBlank()
        )
    }

    suspend fun downloadImageToFile(imageUrl: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .apply {
                    cookieStore.getCookieHeaderForHttp()?.let { header("Cookie", it) }
                }
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use false
                val bytes = resp.body?.bytes() ?: return@use false
                targetFile.parentFile?.mkdirs()
                targetFile.writeBytes(bytes)
                true
            }
        }.getOrDefault(false)
    }

    private fun buildRequest(url: String, cookieHeader: String?, acceptJson: Boolean): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("X-IG-App-ID", "936619743392459")
            .header("Accept-Language", "en-US,en;q=0.9")

        if (acceptJson) {
            builder.header("Accept", "application/json")
        } else {
            builder.header("Accept", "text/html,application/xhtml+xml")
        }
        if (!cookieHeader.isNullOrBlank()) {
            builder.header("Cookie", cookieHeader)
        }
        return builder.build()
    }

    private fun parseInstagramJson(rawJson: String): ExtractedInstagramImages? {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
        val urls = linkedSetOf<String>()
        var title = "Instagram Photo"
        var author: String? = null

        // Format A: items[0] (newer web API response)
        val items = root.optJSONArray("items")
        if (items != null && items.length() > 0) {
            val first = items.optJSONObject(0)
            author = first?.optJSONObject("user")?.optString("username")?.takeIf { it.isNotBlank() }
            val caption = first?.optJSONObject("caption")?.optString("text")?.takeIf { it.isNotBlank() }
            if (caption != null) title = caption.lineSequence().first().take(80)
            else if (author != null) title = "Photo by @$author"

            val carousel = first?.optJSONArray("carousel_media")
            if (carousel != null && carousel.length() > 0) {
                for (i in 0 until carousel.length()) {
                    extractBestCandidateUrl(carousel.optJSONObject(i))?.let { urls.add(it) }
                }
            } else {
                extractBestCandidateUrl(first)?.let { urls.add(it) }
            }
        }

        // Format B: graphql.shortcode_media
        val shortcode = root.optJSONObject("graphql")?.optJSONObject("shortcode_media")
        if (shortcode != null) {
            author = shortcode.optJSONObject("owner")?.optString("username")?.takeIf { it.isNotBlank() } ?: author
            if (author != null && title == "Instagram Photo") title = "Photo by @$author"

            val edges = shortcode.optJSONObject("edge_sidecar_to_children")?.optJSONArray("edges")
            if (edges != null && edges.length() > 0) {
                for (i in 0 until edges.length()) {
                    val node = edges.optJSONObject(i)?.optJSONObject("node")
                    val displayUrl = node?.optString("display_url")?.takeIf { it.isNotBlank() }
                    if (displayUrl != null) urls.add(displayUrl)
                }
            } else {
                val displayUrl = shortcode.optString("display_url").takeIf { it.isNotBlank() }
                if (displayUrl != null) urls.add(displayUrl)
            }
        }

        return ExtractedInstagramImages(
            title = title,
            author = author,
            imageUrls = urls.toList(),
            requiresLogin = urls.isEmpty()
        )
    }

    private fun extractBestCandidateUrl(node: JSONObject?): String? {
        val candidates: JSONArray = node
            ?.optJSONObject("image_versions2")
            ?.optJSONArray("candidates")
            ?: return null

        var bestUrl: String? = null
        var bestArea = -1
        for (i in 0 until candidates.length()) {
            val c = candidates.optJSONObject(i) ?: continue
            val url = c.optString("url")
            val area = c.optInt("width", 0) * c.optInt("height", 0)
            if (url.isNotBlank() && area >= bestArea) {
                bestArea = area
                bestUrl = url
            }
        }
        return bestUrl
    }

    private fun parseInstagramHtml(html: String): ExtractedInstagramImages {
        val urls = linkedSetOf<String>()

        // Match high-res display_url entries embedded in page scripts
        val displayUrlRegex = Regex(""""display_url"\s*:\s*"(https:[^"]+)"""")
        displayUrlRegex.findAll(html).forEach { match ->
            val unescaped = match.groupValues[1]
                .replace("\\u0026", "&")
                .replace("\\/", "/")
            if (unescaped.startsWith("https://")) urls.add(unescaped)
        }

        // Match og:image meta tag
        val ogImageRegex = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        ogImageRegex.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
            val decoded = raw.replace("&amp;", "&")
            if (decoded.startsWith("https://")) urls.add(decoded)
        }

        val ogTitleRegex = Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val parsedTitle = ogTitleRegex.find(html)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.replace("&#x201c;", "\"")
            ?.replace("&#x201d;", "\"")
            ?.take(80)
            ?: "Instagram Photo"

        return ExtractedInstagramImages(
            title = parsedTitle,
            author = null,
            imageUrls = urls.toList(),
            requiresLogin = urls.isEmpty()
        )
    }

    companion object {
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
