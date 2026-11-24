package com.vela.player.data.downloader

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified link analyzer for YouTube (videos, Shorts), Instagram (Reels, Stories, Images, Carousels)
 * and Dailymotion (ad-free HLS streaming + downloads).
 */
class LinkAnalyzer(
    private val cookieStore: InstagramCookieStore,
    private val instagramExtractor: InstagramMediaExtractor
) {
    fun extractSupportedUrl(rawInput: String): String? {
        val urlRegex = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
        val candidate = urlRegex.find(rawInput.trim())?.value ?: rawInput.trim()
        val platform = detectPlatformFromUrl(candidate)
        return when (platform) {
            DownloadPlatform.UNSUPPORTED -> null
            DownloadPlatform.DAILYMOTION -> normalizeDailymotionUrl(candidate)
            else -> candidate
        }
    }

    fun detectPlatform(url: String): DownloadPlatform = detectPlatformFromUrl(url)

    suspend fun analyze(rawInput: String): AnalyzedMedia = withContext(Dispatchers.IO) {
        val url = extractSupportedUrl(rawInput)
            ?: return@withContext AnalyzedMedia(
                url = rawInput.trim(),
                platform = DownloadPlatform.UNSUPPORTED,
                title = "Unsupported link",
                errorMessage = "Please paste a valid YouTube, Instagram or Dailymotion URL."
            )

        val platform = detectPlatform(url)
        val isStoryUrl = platform == DownloadPlatform.INSTAGRAM && url.lowercase().contains("/stories/")

        // Stories almost always require an authenticated Instagram session
        if (isStoryUrl && !cookieStore.hasValidSession()) {
            return@withContext AnalyzedMedia(
                url = url,
                platform = DownloadPlatform.INSTAGRAM,
                title = "Instagram Story",
                requiresInstagramLogin = true,
                errorMessage = "Instagram Stories require logging in with your Instagram account."
            )
        }

        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--no-warnings")
            if (platform == DownloadPlatform.DAILYMOTION) {
                // The metadata API exposes the plain HLS stream, which carries no ads.
                addOption("-f", "best[protocol^=m3u8][height<=?1080]/best")
            }
            if (platform == DownloadPlatform.INSTAGRAM) {
                cookieStore.getCookieFileIfPresent()?.let { cookieFile ->
                    addOption("--cookies", cookieFile.absolutePath)
                }
            }
        }

        val ytResult = runCatching {
            YoutubeDL.getInstance().getInfo(request)
        }

        if (ytResult.isSuccess) {
            val info = ytResult.getOrThrow()
            val title = info.title?.takeIf { it.isNotBlank() } ?: when (platform) {
                DownloadPlatform.YOUTUBE -> "YouTube Video"
                DownloadPlatform.INSTAGRAM -> "Instagram Video"
                DownloadPlatform.DAILYMOTION -> "Dailymotion Video"
                DownloadPlatform.UNSUPPORTED -> "Media"
            }
            val options = listOf(
                DownloadOption.VIDEO_360P,
                DownloadOption.VIDEO_720P,
                DownloadOption.VIDEO_1080P,
                DownloadOption.AUDIO_ONLY
            )

            // For Instagram posts, also probe if there are carousel images we can offer
            val extraImages = if (platform == DownloadPlatform.INSTAGRAM && url.contains("/p/")) {
                runCatching { instagramExtractor.extractImages(url).imageUrls }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            return@withContext AnalyzedMedia(
                url = url,
                platform = platform,
                title = title,
                author = info.uploader,
                thumbnailUrl = info.thumbnail,
                durationSec = info.duration.toLong().coerceAtLeast(0L),
                availableOptions = if (extraImages.isNotEmpty()) options + DownloadOption.IMAGE_ORIGINAL else options,
                imageUrls = extraImages,
                streamUrl = if (platform == DownloadPlatform.DAILYMOTION) resolveStreamUrl(info) else null,
                streamHeaders = if (platform == DownloadPlatform.DAILYMOTION) resolveStreamHeaders(info) else emptyMap()
            )
        }

        // Handle yt-dlp errors gracefully
        val errText = ytResult.exceptionOrNull()?.message.orEmpty()
        val lowerErr = errText.lowercase()

        if (platform == DownloadPlatform.INSTAGRAM) {
            val looksLikeImageOnly = lowerErr.contains("no video formats") ||
                lowerErr.contains("there is no video") ||
                lowerErr.contains("unsupported url")

            if (looksLikeImageOnly) {
                val extracted = instagramExtractor.extractImages(url)
                if (extracted.imageUrls.isNotEmpty()) {
                    return@withContext AnalyzedMedia(
                        url = url,
                        platform = DownloadPlatform.INSTAGRAM,
                        title = extracted.title,
                        author = extracted.author,
                        thumbnailUrl = extracted.imageUrls.firstOrNull(),
                        availableOptions = listOf(DownloadOption.IMAGE_ORIGINAL),
                        imageUrls = extracted.imageUrls
                    )
                }
                if (extracted.requiresLogin) {
                    return@withContext AnalyzedMedia(
                        url = url,
                        platform = DownloadPlatform.INSTAGRAM,
                        title = "Instagram Login Required",
                        requiresInstagramLogin = true,
                        errorMessage = "Log in to Instagram to access this photo, story, or private post."
                    )
                }
            }

            val looksLikeAuthError = lowerErr.contains("login") ||
                lowerErr.contains("cookies") ||
                lowerErr.contains("private") ||
                lowerErr.contains("rate-limit") ||
                lowerErr.contains("requested content is not available") ||
                lowerErr.contains("401") ||
                lowerErr.contains("403")

            if (looksLikeAuthError) {
                return@withContext AnalyzedMedia(
                    url = url,
                    platform = DownloadPlatform.INSTAGRAM,
                    title = "Instagram Login Required",
                    requiresInstagramLogin = true,
                    errorMessage = "Instagram asked for sign-in to load this link. Tap Login to authenticate."
                )
            }
        }

        AnalyzedMedia(
            url = url,
            platform = platform,
            title = if (looksLikeOutdatedEngine(errText)) "Engine update needed" else "Could not inspect link",
            errorMessage = friendlyErrorMessage(errText)
        )
    }

    /** yt-dlp extractor breakage (site changed) is fixed by updating the bundled engine. */
    private fun looksLikeOutdatedEngine(err: String): Boolean {
        val l = err.lowercase()
        return l.contains("no video formats") ||
            l.contains("unable to extract") ||
            l.contains("please report this issue") ||
            l.contains("unsupported url") ||
            l.contains("requested format is not available")
    }

    private fun friendlyErrorMessage(err: String): String {
        val l = err.lowercase()
        return when {
            looksLikeOutdatedEngine(err) ->
                "The yt-dlp engine can't read this link — it's most likely out of date. " +
                    "Go to Settings → Update yt-dlp engine, then paste the link again."
            l.contains("unable to resolve host") || l.contains("timed out") ||
                l.contains("network is unreachable") || l.contains("failed to connect") ->
                "Couldn't reach the server. Check your internet connection and try again."
            err.isNotBlank() ->
                "${err.take(140)}\n\nIf this keeps happening, try Settings → Update yt-dlp engine."
            else ->
                "Unable to load media info. Check your internet connection or update the yt-dlp engine in Settings."
        }
    }

    /**
     * Prefers the HLS master manifest (lets ExoPlayer adapt quality); falls back to the selected
     * format's URL, then to the tallest listed format.
     */
    private fun resolveStreamUrl(info: VideoInfo): String? {
        info.manifestUrl?.takeIf { it.isNotBlank() }?.let { return it }
        info.url?.takeIf { it.isNotBlank() }?.let { return it }
        return info.formats
            ?.filter { !it.url.isNullOrBlank() }
            ?.maxByOrNull { it.height }
            ?.url
    }

    private fun resolveStreamHeaders(info: VideoInfo): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        info.httpHeaders?.forEach { (k, v) -> if (k != null && v != null) headers[k] = v }
        if (headers.keys.none { it.equals("Referer", ignoreCase = true) }) {
            headers["Referer"] = "https://www.dailymotion.com/"
        }
        return headers
    }

    companion object {
        private val dailymotionVideoId =
            Regex("""(?:dailymotion\.com/(?:embed/)?video/|dai\.ly/)([a-z0-9]+)""", RegexOption.IGNORE_CASE)
        private val dailymotionGeoVideoParam =
            Regex("""[?&]video=([a-z0-9]+)""", RegexOption.IGNORE_CASE)

        /** Pure platform detection, kept free of Android types so it is unit-testable. */
        fun detectPlatformFromUrl(url: String): DownloadPlatform {
            val lower = url.lowercase()
            return when {
                lower.contains("youtube.com/") || lower.contains("youtu.be/") -> DownloadPlatform.YOUTUBE
                lower.contains("instagram.com/") || lower.contains("instagr.am/") -> DownloadPlatform.INSTAGRAM
                lower.contains("dailymotion.com/") || lower.contains("dai.ly/") -> DownloadPlatform.DAILYMOTION
                else -> DownloadPlatform.UNSUPPORTED
            }
        }

        /**
         * Rewrites short (dai.ly), embed and geo-player links to https://www.dailymotion.com/video/<id>,
         * dropping playlist/query noise. Returns the input unchanged when no video id is found.
         */
        fun normalizeDailymotionUrl(url: String): String {
            val id = if (url.contains("geo.dailymotion.com", ignoreCase = true)) {
                dailymotionGeoVideoParam.find(url)?.groupValues?.get(1)
            } else {
                dailymotionVideoId.find(url)?.groupValues?.get(1)
            }
            return if (id.isNullOrBlank()) url else "https://www.dailymotion.com/video/$id"
        }
    }
}
