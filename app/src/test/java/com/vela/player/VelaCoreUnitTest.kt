package com.vela.player

import com.vela.player.data.downloader.DownloadOption
import com.vela.player.data.downloader.DownloadPlatform
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
    fun platformDetectionRecognizesYoutubeAndInstagram() {
        val lowerCheck: (String) -> DownloadPlatform = { url ->
            val l = url.lowercase()
            when {
                l.contains("youtube.com/") || l.contains("youtu.be/") -> DownloadPlatform.YOUTUBE
                l.contains("instagram.com/") || l.contains("instagr.am/") -> DownloadPlatform.INSTAGRAM
                else -> DownloadPlatform.UNSUPPORTED
            }
        }
        assertEquals(DownloadPlatform.YOUTUBE, lowerCheck("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(DownloadPlatform.YOUTUBE, lowerCheck("https://www.youtube.com/shorts/abc123"))
        assertEquals(DownloadPlatform.INSTAGRAM, lowerCheck("https://www.instagram.com/reel/CxYz123/"))
        assertEquals(DownloadPlatform.INSTAGRAM, lowerCheck("https://www.instagram.com/stories/user/12345/"))
        assertEquals(DownloadPlatform.INSTAGRAM, lowerCheck("https://www.instagram.com/p/CxYz123/"))
        assertEquals(DownloadPlatform.UNSUPPORTED, lowerCheck("https://example.com/file"))
    }
}
