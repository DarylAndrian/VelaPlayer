package com.vela.player

import com.vela.player.data.downloader.DownloadOption
import com.vela.player.data.downloader.DownloadPlatform
import com.vela.player.data.downloader.LinkAnalyzer
import com.vela.player.player.VelaPlayerController
import com.vela.player.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VelaCoreUnitTest {

    @Test
    fun speedPresetsMatchSpecification() {
        assertEquals(
            listOf(1f, 1.25f, 1.5f, 1.75f, 2f, 4f, 5f, 10f),
            VelaPlayerController.SPEED_PRESETS
        )
    }

    @Test
    fun formatUtilsFormatsDurationsAndSpeeds() {
        assertEquals("0:00", FormatUtils.formatDuration(0L))
        assertEquals("1:05", FormatUtils.formatDuration(65_000L))
        assertEquals("1:01:05", FormatUtils.formatDuration(3_665_000L))
        assertEquals("1x", FormatUtils.formatSpeedLabel(1f))
        assertEquals("1.25x", FormatUtils.formatSpeedLabel(1.25f))
        assertEquals("10x", FormatUtils.formatSpeedLabel(10f))
    }

    @Test
    fun downloadOptionsIncludeResolutionsAudioAndImage() {
        val ids = DownloadOption.entries.map { it.id }
        assertTrue(ids.containsAll(listOf("360", "720", "1080", "AUDIO", "IMAGE")))
    }

    @Test
    fun platformDetectionRecognizesYoutubeInstagramAndDailymotion() {
        val detect = LinkAnalyzer.Companion::detectPlatformFromUrl
        assertEquals(DownloadPlatform.YOUTUBE, detect("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(DownloadPlatform.YOUTUBE, detect("https://www.youtube.com/shorts/abc123"))
        assertEquals(DownloadPlatform.INSTAGRAM, detect("https://www.instagram.com/reel/CxYz123/"))
        assertEquals(DownloadPlatform.INSTAGRAM, detect("https://www.instagram.com/stories/user/12345/"))
        assertEquals(DownloadPlatform.INSTAGRAM, detect("https://www.instagram.com/p/CxYz123/"))
        assertEquals(DownloadPlatform.DAILYMOTION, detect("https://www.dailymotion.com/video/x8abc12"))
        assertEquals(DownloadPlatform.DAILYMOTION, detect("https://dai.ly/x8abc12"))
        assertEquals(DownloadPlatform.DAILYMOTION, detect("https://www.dailymotion.com/embed/video/x8abc12"))
        assertEquals(DownloadPlatform.UNSUPPORTED, detect("https://example.com/file"))
    }

    @Test
    fun dailymotionUrlsNormalizeToCanonicalVideoLink() {
        val canonical = "https://www.dailymotion.com/video/x8abc12"
        val normalize = LinkAnalyzer.Companion::normalizeDailymotionUrl
        assertEquals(canonical, normalize("https://www.dailymotion.com/video/x8abc12"))
        assertEquals(canonical, normalize("https://www.dailymotion.com/video/x8abc12_some-title-slug"))
        assertEquals(canonical, normalize("https://www.dailymotion.com/video/x8abc12?playlist=x6xyz&autoplay=1"))
        assertEquals(canonical, normalize("https://dai.ly/x8abc12"))
        assertEquals(canonical, normalize("https://www.dailymotion.com/embed/video/x8abc12?autoPlay=1"))
        assertEquals(canonical, normalize("https://geo.dailymotion.com/player/xabc.html?video=x8abc12"))
        val untouched = "https://www.dailymotion.com/playlist/x6xyz"
        assertEquals(untouched, normalize(untouched))
    }
}
