package com.vela.player.ui.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.vela.player.R
import com.vela.player.VelaPlayerApp
import com.vela.player.data.downloader.AnalyzedMedia
import com.vela.player.data.local.MediaSource
import com.vela.player.data.local.MediaItemEntity
import com.vela.player.data.local.MediaType
import com.vela.player.data.local.UserSettings
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.vela.player.player.PlaybackQueueMode
import com.vela.player.player.VelaPlayerController
import com.vela.player.ui.theme.VelaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Dedicated PiP-enabled Offline Player Activity.
 * Runs in singleTask mode with supportsPictureInPicture="true" so the video can float
 * over VelaPlayer's own Home/Gallery/Downloader screens as well as the Android OS Home screen.
 */
class PlayerActivity : ComponentActivity() {

    private lateinit var controller: VelaPlayerController
    private val app get() = application as VelaPlayerApp

    private val currentMedia = MutableStateFlow<MediaItemEntity?>(null)
    private val queue = MutableStateFlow<List<MediaItemEntity>>(emptyList())
    private val queueMode = MutableStateFlow(PlaybackQueueMode.IN_ORDER)
    private var isInPip by mutableStateOf(false)
    private var isScreenLocked by mutableStateOf(false)
    private var rotationStep by mutableIntStateOf(0)
    private var handlingQueueAdvance = false
    private var lastQueueAdvanceAt = 0L
    private val playedHistory = ArrayDeque<Long>()

    private val playbackEndListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                advanceQueue()
            }
        }
    }

    /** Set while playing a resolved web stream, so an expired signed URL can be re-resolved once. */
    private var streamSourceUrl: String? = null
    private var streamRefreshed = false

    private val streamErrorListener = object : Player.Listener {
        @OptIn(UnstableApi::class)
        override fun onPlayerError(error: PlaybackException) {
            val source = streamSourceUrl ?: return
            val cause = error.cause
            val expired = cause is HttpDataSource.InvalidResponseCodeException &&
                (cause.responseCode == 403 || cause.responseCode == 410)
            if (expired && !streamRefreshed) {
                streamRefreshed = true
                refreshStream(source)
            }
        }
    }

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(EXTRA_PIP_ACTION)) {
                PIP_ACTION_TOGGLE -> {
                    controller.togglePlayPause()
                    updatePipParams()
                }
                PIP_ACTION_REWIND -> controller.seekBy(-VelaPlayerController.SKIP_STEP_MS)
                PIP_ACTION_FORWARD -> controller.seekBy(VelaPlayerController.SKIP_STEP_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        controller = VelaPlayerController(this)
        controller.exoPlayer.addListener(playbackEndListener)
        controller.exoPlayer.addListener(streamErrorListener)
        rotationStep = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0
        registerPipReceiver()
        handleIntent(intent)
        lifecycleScope.launch {
            queueMode.value = app.preferences.settings.first().queueMode
        }

        lifecycleScope.launch {
            controller.state.collect {
                updatePipParams()
            }
        }

        setContent {
            val userSettings by app.preferences.settings.collectAsState(initial = UserSettings())
            val media by currentMedia.collectAsState()
            val playbackState by controller.state.collectAsState()
            val mode by queueMode.collectAsState()
            val queueItems by queue.collectAsState()
            val upNextTitle = nextInQueue(media, queueItems, mode)?.title

            VelaTheme(themeMode = userSettings.themeMode, forceSystemBarContrast = false) {
                PlayerScreen(
                    controller = controller,
                    playbackState = playbackState,
                    title = media?.title ?: "Offline Media",
                    isAudioOnly = media?.parsedMediaType == MediaType.AUDIO,
                    isStarred = media?.isStarred == true,
                    isInPipMode = isInPip,
                    isScreenLocked = isScreenLocked,
                    queueMode = mode,
                    upNextTitle = upNextTitle,
                    onQueueModeChange = { selected ->
                        queueMode.value = selected
                        lifecycleScope.launch { app.preferences.setQueueMode(selected) }
                    },
                    onRotateScreen = { rotateScreen() },
                    onPrevious = { goToPrevious() },
                    onNext = { goToNext() },
                    onScreenLockChange = { locked ->
                        isScreenLocked = locked
                        updatePipParams()
                    },
                    onToggleStar = {
                        val item = currentMedia.value ?: return@PlayerScreen
                        val nextStar = !item.isStarred
                        currentMedia.value = item.copy(isStarred = nextStar)
                        lifecycleScope.launch {
                            app.database.mediaDao().setStarred(item.id, nextStar)
                        }
                    },
                    onEnterPip = { enterPipNow() },
                    onClose = {
                        persistResumePosition()
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        persistResumePosition()
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // On Android < 12, auto-enter floating PiP when user presses Home (unless screen is locked)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isScreenLocked && controller.exoPlayer.isPlaying) {
            enterPipNow()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            hideSystemBars()
        }
    }

    override fun onPause() {
        super.onPause()
        persistResumePosition()
        // Keep playing seamlessly while in PiP; pause only when truly backgrounded without PiP
        if (!isInPictureInPictureMode && !isChangingConfigurations) {
            controller.exoPlayer.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        persistResumePosition()
        if (!isInPictureInPictureMode) {
            controller.exoPlayer.pause()
        }
    }

    override fun onDestroy() {
        persistResumePosition()
        controller.exoPlayer.removeListener(playbackEndListener)
        controller.exoPlayer.removeListener(streamErrorListener)
        runCatching { unregisterReceiver(pipActionReceiver) }
        controller.release()
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        val mediaId = intent?.getLongExtra(EXTRA_MEDIA_ID, -1L) ?: -1L
        val directPath = intent?.getStringExtra(EXTRA_MEDIA_PATH)
        val directTitle = intent?.getStringExtra(EXTRA_MEDIA_TITLE)
        val requestedQueue = intent?.getLongArrayExtra(EXTRA_QUEUE_IDS)?.toList().orEmpty()
        val streamUrl = intent?.getStringExtra(EXTRA_STREAM_URL)
        streamSourceUrl = if (streamUrl.isNullOrBlank()) null else intent?.getStringExtra(EXTRA_SOURCE_URL)
        streamRefreshed = false
        if (!streamUrl.isNullOrBlank()) {
            val headers = intent?.getBundleExtra(EXTRA_STREAM_HEADERS)
                ?.let { b -> b.keySet().associateWith { b.getString(it).orEmpty() } }
                .orEmpty()
            queue.value = emptyList()
            currentMedia.value = MediaItemEntity(
                id = -1L,
                title = directTitle ?: "Dailymotion Video",
                filePath = streamUrl,
                sourcePlatform = MediaSource.DAILYMOTION.name,
                sourceUrl = streamSourceUrl
            )
            controller.loadMedia(streamUrl, 0L, headers, isHls = true)
            return
        }

        lifecycleScope.launch {
            queue.value = loadQueue(requestedQueue, mediaId)
            val loaded = if (mediaId > 0L) {
                app.database.mediaDao().getById(mediaId)
            } else {
                null
            }
            if (loaded != null) {
                currentMedia.value = loaded
                val resumeMs = if (loaded.durationMs > 0L && loaded.lastPositionMs < loaded.durationMs - 3_000L) {
                    loaded.lastPositionMs
                } else {
                    0L
                }
                controller.loadMedia(loaded.filePath, resumeMs)
            } else if (!directPath.isNullOrBlank()) {
                currentMedia.value = MediaItemEntity(
                    id = -1L,
                    title = directTitle ?: "Offline Video",
                    filePath = directPath
                )
                controller.loadMedia(directPath, 0L)
            } else {
                finish()
            }
        }
    }

    /** Signed stream URLs expire after a few hours; fetch a fresh one and resume where we were. */
    private fun refreshStream(sourceUrl: String) {
        val resumeMs = controller.exoPlayer.currentPosition.coerceAtLeast(0L)
        lifecycleScope.launch {
            val fresh = app.linkAnalyzer.analyze(sourceUrl)
            val url = fresh.streamUrl ?: return@launch
            controller.loadMedia(url, resumeMs, fresh.streamHeaders, isHls = true)
        }
    }

    private suspend fun loadQueue(requestedIds: List<Long>, currentId: Long): List<MediaItemEntity> {
        val dao = app.database.mediaDao()
        val loaded = if (requestedIds.isNotEmpty()) {
            val byId = dao.getByIds(requestedIds).associateBy { it.id }
            requestedIds.mapNotNull { byId[it] }
        } else {
            dao.getPlayable()
        }.filter { it.parsedMediaType != MediaType.IMAGE }

        if (currentId > 0L && loaded.none { it.id == currentId }) {
            val current = dao.getById(currentId)
            if (current != null && current.parsedMediaType != MediaType.IMAGE) {
                return listOf(current) + loaded
            }
        }
        return loaded
    }

    private fun nextInQueue(
        current: MediaItemEntity?,
        items: List<MediaItemEntity>,
        mode: PlaybackQueueMode
    ): MediaItemEntity? {
        if (current == null || items.isEmpty() || mode == PlaybackQueueMode.REPEAT_ONE) return null
        if (items.size == 1) return items.first()
        val index = items.indexOfFirst { it.id == current.id }
        return when (mode) {
            PlaybackQueueMode.SHUFFLE -> items.filter { it.id != current.id }.randomOrNull()
            PlaybackQueueMode.IN_ORDER -> items[(if (index < 0) 0 else index + 1) % items.size]
            PlaybackQueueMode.REPEAT_ONE -> null
        }
    }

    private fun advanceQueue() {
        if (handlingQueueAdvance) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastQueueAdvanceAt < 800L) return
        lastQueueAdvanceAt = now
        val current = currentMedia.value ?: return
        val mode = queueMode.value
        if (mode == PlaybackQueueMode.REPEAT_ONE || queue.value.size <= 1 && mode != PlaybackQueueMode.IN_ORDER) {
            handlingQueueAdvance = true
            controller.replayFromStart()
            handlingQueueAdvance = false
            return
        }
        val next = nextInQueue(current, queue.value, mode) ?: return
        if (next.id == current.id) {
            handlingQueueAdvance = true
            controller.replayFromStart()
            handlingQueueAdvance = false
            return
        }
        playQueueItem(next, rememberCurrent = true)
    }

    private fun goToNext() {
        val current = currentMedia.value ?: return
        val navigationMode = if (queueMode.value == PlaybackQueueMode.REPEAT_ONE) {
            PlaybackQueueMode.IN_ORDER
        } else {
            queueMode.value
        }
        val next = nextInQueue(current, queue.value, navigationMode) ?: return
        playQueueItem(next, rememberCurrent = true)
    }

    private fun goToPrevious() {
        val current = currentMedia.value ?: return
        val fromHistory = playedHistory.removeLastOrNull()?.let { id ->
            queue.value.firstOrNull { it.id == id }
        }
        val previous = fromHistory ?: previousInOrder(current) ?: return
        playQueueItem(previous, rememberCurrent = false)
    }

    private fun previousInOrder(current: MediaItemEntity): MediaItemEntity? {
        val items = queue.value
        if (items.isEmpty()) return null
        if (items.size == 1) return items.first()
        val index = items.indexOfFirst { it.id == current.id }
        val previousIndex = if (index <= 0) items.lastIndex else index - 1
        return items[previousIndex]
    }

    private fun playQueueItem(item: MediaItemEntity, rememberCurrent: Boolean) {
        val current = currentMedia.value
        if (rememberCurrent && current != null && current.id > 0L && current.id != item.id) {
            playedHistory.addLast(current.id)
        }
        if (item.id == current?.id) {
            controller.replayFromStart()
            return
        }
        handlingQueueAdvance = true
        persistResumePosition()
        currentMedia.value = item
        controller.loadMedia(item.filePath, 0L)
        handlingQueueAdvance = false
    }

    private fun rotateScreen() {
        rotationStep = (rotationStep + 1) % 4
        requestedOrientation = when (rotationStep) {
            0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            2 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
    }

    private fun enterPipNow() {
        runCatching {
            enterPictureInPictureMode(buildPipParams())
        }
    }

    private fun updatePipParams() {
        runCatching {
            setPictureInPictureParams(buildPipParams())
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val playback = controller.state.value
        val actions = listOf(
            createRemoteAction(
                requestCode = 1,
                iconRes = R.drawable.ic_pip_rewind,
                title = getString(R.string.pip_action_rewind),
                actionName = PIP_ACTION_REWIND
            ),
            createRemoteAction(
                requestCode = 2,
                iconRes = if (playback.isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play,
                title = getString(if (playback.isPlaying) R.string.pip_action_pause else R.string.pip_action_play),
                actionName = PIP_ACTION_TOGGLE
            ),
            createRemoteAction(
                requestCode = 3,
                iconRes = R.drawable.ic_pip_forward,
                title = getString(R.string.pip_action_forward),
                actionName = PIP_ACTION_FORWARD
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(playback.videoAspectRatio)
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(!isScreenLocked && playback.isPlaying)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun createRemoteAction(
        requestCode: Int,
        iconRes: Int,
        title: String,
        actionName: String
    ): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL)
            .setPackage(packageName)
            .putExtra(EXTRA_PIP_ACTION, actionName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pendingIntent)
    }

    private fun registerPipReceiver() {
        val filter = IntentFilter(ACTION_PIP_CONTROL)
        ContextCompat.registerReceiver(
            this,
            pipActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun persistResumePosition() {
        val item = currentMedia.value ?: return
        if (item.id <= 0L) return
        val pos = controller.exoPlayer.currentPosition.coerceAtLeast(0L)
        lifecycleScope.launch {
            app.database.mediaDao().updatePlaybackPosition(item.id, pos)
        }
    }

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        private const val EXTRA_MEDIA_ID = "extra_media_id"
        private const val EXTRA_MEDIA_PATH = "extra_media_path"
        private const val EXTRA_MEDIA_TITLE = "extra_media_title"
        private const val EXTRA_QUEUE_IDS = "extra_queue_ids"
        private const val EXTRA_STREAM_URL = "extra_stream_url"
        private const val EXTRA_STREAM_HEADERS = "extra_stream_headers"
        private const val EXTRA_SOURCE_URL = "extra_source_url"
        private const val ACTION_PIP_CONTROL = "com.vela.player.ACTION_PIP_CONTROL"
        private const val EXTRA_PIP_ACTION = "extra_pip_action"
        private const val PIP_ACTION_TOGGLE = "toggle"
        private const val PIP_ACTION_REWIND = "rewind"
        private const val PIP_ACTION_FORWARD = "forward"

        fun createIntent(
            context: Context,
            mediaItem: MediaItemEntity,
            queueIds: LongArray = longArrayOf()
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_ID, mediaItem.id)
                putExtra(EXTRA_MEDIA_PATH, mediaItem.filePath)
                putExtra(EXTRA_MEDIA_TITLE, mediaItem.title)
                putExtra(EXTRA_QUEUE_IDS, queueIds)
            }
        }

        /** Streams an ad-free web video (resolved by [com.vela.player.data.downloader.LinkAnalyzer]). */
        fun createStreamIntent(context: Context, media: AnalyzedMedia): Intent {
            val headers = Bundle().apply {
                media.streamHeaders.forEach { (k, v) -> putString(k, v) }
            }
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, media.streamUrl)
                putExtra(EXTRA_STREAM_HEADERS, headers)
                putExtra(EXTRA_SOURCE_URL, media.url)
                putExtra(EXTRA_MEDIA_TITLE, media.title)
            }
        }
    }
}
