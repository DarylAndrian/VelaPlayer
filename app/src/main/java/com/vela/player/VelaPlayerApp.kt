package com.vela.player

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.vela.player.data.downloader.DownloadRepository
import com.vela.player.data.downloader.InstagramCookieStore
import com.vela.player.data.downloader.InstagramMediaExtractor
import com.vela.player.data.downloader.LinkAnalyzer
import com.vela.player.data.local.LocalMediaScanner
import com.vela.player.data.local.UserPreferencesRepository
import com.vela.player.data.local.VelaDatabase
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VelaPlayerApp : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: VelaDatabase
        private set
    lateinit var preferences: UserPreferencesRepository
        private set
    lateinit var instagramCookieStore: InstagramCookieStore
        private set
    lateinit var instagramMediaExtractor: InstagramMediaExtractor
        private set
    lateinit var linkAnalyzer: LinkAnalyzer
        private set
    lateinit var downloadRepository: DownloadRepository
        private set
    lateinit var localMediaScanner: LocalMediaScanner
        private set

    private val _engineReady = MutableStateFlow(false)
    val engineReady: StateFlow<Boolean> = _engineReady.asStateFlow()

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        database = VelaDatabase.getInstance(this)
        preferences = UserPreferencesRepository(this)
        instagramCookieStore = InstagramCookieStore(this)
        instagramMediaExtractor = InstagramMediaExtractor(instagramCookieStore)
        linkAnalyzer = LinkAnalyzer(instagramCookieStore, instagramMediaExtractor)
        downloadRepository = DownloadRepository(
            context = this,
            mediaDao = database.mediaDao(),
            cookieStore = instagramCookieStore,
            instagramExtractor = instagramMediaExtractor
        )
        localMediaScanner = LocalMediaScanner(this, database.mediaDao())

        // Initialize bundled yt-dlp + ffmpeg binaries off the main thread for instant launch
        appScope.launch {
            runCatching {
                YoutubeDL.getInstance().init(this@VelaPlayerApp)
                FFmpeg.getInstance().init(this@VelaPlayerApp)
            }.onSuccess {
                _engineReady.value = true
                _engineError.value = null
            }.onFailure { err ->
                Log.e(TAG, "Failed to initialize yt-dlp/ffmpeg", err)
                _engineError.value = err.message ?: "Downloader engine failed to initialize"
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    companion object {
        private const val TAG = "VelaPlayerApp"
    }
}
