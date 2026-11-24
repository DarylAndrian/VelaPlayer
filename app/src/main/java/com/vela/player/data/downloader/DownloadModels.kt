package com.vela.player.data.downloader

enum class DownloadPlatform(val label: String) {
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram"),
    DAILYMOTION("Dailymotion"),
    UNSUPPORTED("Unsupported")
}

enum class DownloadOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val maxHeight: Int? = null,
    val isAudioOnly: Boolean = false,
    val isImage: Boolean = false
) {
    VIDEO_360P("360", "360p", "Compact MP4", maxHeight = 360),
    VIDEO_720P("720", "720p HD", "Balanced MP4", maxHeight = 720),
    VIDEO_1080P("1080", "1080p Full HD", "High quality MP4", maxHeight = 1080),
    AUDIO_ONLY("AUDIO", "Audio Only", "M4A soundtrack", isAudioOnly = true),
    IMAGE_ORIGINAL("IMAGE", "Save Image(s)", "Original resolution JPG", isImage = true);

    companion object {
        fun fromId(id: String): DownloadOption =
            entries.firstOrNull { it.id == id } ?: VIDEO_720P
    }
}

data class AnalyzedMedia(
    val url: String,
    val platform: DownloadPlatform,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Long = 0L,
    val availableOptions: List<DownloadOption> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val requiresInstagramLogin: Boolean = false,
    val errorMessage: String? = null,
    /** Direct ad-free stream (HLS) resolved by yt-dlp, when the platform supports instant watching. */
    val streamUrl: String? = null,
    val streamHeaders: Map<String, String> = emptyMap()
)

enum class DownloadStatus(val label: String) {
    QUEUED("Queued"),
    DOWNLOADING("Downloading"),
    MERGING("Processing"),
    COMPLETED("Saved"),
    FAILED("Failed")
}

data class DownloadJob(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val platform: DownloadPlatform,
    val option: DownloadOption,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val statusDetail: String = "Waiting to start",
    val outputPath: String? = null
)
