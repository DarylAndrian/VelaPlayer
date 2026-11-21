package com.vela.player.data.downloader

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified link analyzer for YouTube (videos, Shorts) and Instagram (Reels, Stories, Images, Carousels).
 */
class LinkAnalyzer(
    private val cookieStore: InstagramCookieStore,
    private val instagramExtractor: InstagramMediaExtractor
) {
    fun extractSupportedUrl(rawInput: String): String? {
        val urlRegex = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
        val candidate = urlRegex.find(rawInput.trim())?.value ?: rawInput.trim()
        return candidate.takeIf { detectPlatform(it) != DownloadPlatform.UNSUPPORTED }
    }

    fun detectPlatform(url: String): DownloadPlatform {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com/") || lower.contains("youtu.be/") -> DownloadPlatform.YOUTUBE
            lower.contains("instagram.com/") || lower.contains("instagr.am/") -> DownloadPlatform.INSTAGRAM
            else -> DownloadPlatform.UNSUPPORTED
        }
    }

    suspend fun analyze(rawInput: String): AnalyzedMedia = withContext(Dispatchers.IO) {
        val url = extractSupportedUrl(rawInput)
            ?: return@withContext AnalyzedMedia(
                url = rawInput.trim(),
                platform = DownloadPlatform.UNSUPPORTED,
                title = "Unsupported link",
                errorMessage = "Please paste a valid YouTube or Instagram URL."
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
                imageUrls = extraImages
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
            title = "Could not inspect link",
            errorMessage = errText.takeIf { it.isNotBlank() }?.take(180)
                ?: "Unable to load media info. Check your internet connection or update the yt-dlp engine in Settings."
        )
    }
}
