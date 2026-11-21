package com.vela.player.player

import android.content.Context
import android.net.Uri
import android.util.Rational
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlayerPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val isAudioDisabledForUltraSpeed: Boolean = false,
    val isMutedByUser: Boolean = false,
    val videoAspectRatio: Rational = Rational(16, 9)
)

/**
 * Wraps AndroidX Media3 ExoPlayer for smooth offline playback, exact 1x–10x speed control,
 * and PiP aspect ratio tracking.
 */
class VelaPlayerController(context: Context) {

    @OptIn(UnstableApi::class)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(PlayerPlaybackState())
    val state: StateFlow<PlayerPlaybackState> = _state.asStateFlow()

    private var userVolumeBeforeMute: Float = 1f

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncSnapshot()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncSnapshot()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            syncSnapshot()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                val safeRatio = clampForAndroidPip(videoSize.width, videoSize.height)
                _state.value = _state.value.copy(videoAspectRatio = safeRatio)
            }
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    fun loadMedia(pathOrUri: String, resumePositionMs: Long = 0L) {
        val uri = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") || pathOrUri.startsWith("http")) {
            Uri.parse(pathOrUri)
        } else {
            Uri.fromFile(File(pathOrUri))
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        if (resumePositionMs > 0L) {
            exoPlayer.seekTo(resumePositionMs)
        }
        exoPlayer.playWhenReady = true
        syncSnapshot()
    }

    fun togglePlayPause() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            exoPlayer.playWhenReady = true
        } else if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        syncSnapshot()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(clamped)
        syncSnapshot()
    }

    fun seekBy(deltaMs: Long) {
        val duration = exoPlayer.duration
        val upper = if (duration > 0L) duration else Long.MAX_VALUE
        val clamped = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, upper)
        exoPlayer.seekTo(clamped)
        syncSnapshot()
    }

    fun replayFromStart() {
        exoPlayer.seekTo(0L)
        exoPlayer.playWhenReady = true
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
        syncSnapshot()
    }

    fun toggleMute() {
        val currentlyMuted = _state.value.isMutedByUser
        if (currentlyMuted) {
            exoPlayer.volume = userVolumeBeforeMute.coerceAtLeast(0.5f)
            _state.value = _state.value.copy(isMutedByUser = false)
        } else {
            userVolumeBeforeMute = exoPlayer.volume
            exoPlayer.volume = 0f
            _state.value = _state.value.copy(isMutedByUser = true)
        }
    }

    /**
     * Supports all 8 requested presets: 1x, 1.25x, 1.5x, 1.75x, 2x, 4x, 5x, 10x.
     *
     * Media3's DefaultAudioSink clamps playback speed to 8.0f while the audio renderer
     * is active. For speeds > 8.0f (10x), we disable C.TRACK_TYPE_AUDIO so the video
     * renderer runs at true 10.0x wall-clock speed, and immediately re-enable audio
     * when returning to <= 8.0f.
     */
    fun setSpeed(speed: Float) {
        val disableAudioForUltraSpeed = speed > MEDIA3_AUDIO_SINK_MAX_SPEED
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, disableAudioForUltraSpeed)
            .build()
        exoPlayer.playbackParameters = PlaybackParameters(speed, 1.0f)
        _state.value = _state.value.copy(
            speed = speed,
            isAudioDisabledForUltraSpeed = disableAudioForUltraSpeed
        )
    }

    fun pollPosition() {
        val current = exoPlayer.currentPosition.coerceAtLeast(0L)
        val total = exoPlayer.duration.coerceAtLeast(0L)
        val prev = _state.value
        if (prev.currentPositionMs != current || prev.durationMs != total) {
            _state.value = prev.copy(
                currentPositionMs = current,
                durationMs = total
            )
        }
    }

    fun syncSnapshot() {
        _state.value = _state.value.copy(
            isPlaying = exoPlayer.isPlaying,
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
        )
    }

    fun release() {
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }

    companion object {
        const val SKIP_STEP_SECONDS = 10
        const val SKIP_STEP_MS = SKIP_STEP_SECONDS * 1_000L
        val SPEED_PRESETS: List<Float> = listOf(1f, 1.25f, 1.5f, 1.75f, 2f, 4f, 5f, 10f)
        private const val MEDIA3_AUDIO_SINK_MAX_SPEED = 8.0f

        /**
         * Android PictureInPictureParams rejects ratios outside [0.418410, 2.39].
         */
        fun clampForAndroidPip(width: Int, height: Int): Rational {
            val ratio = width.toFloat() / height.toFloat()
            return when {
                ratio < 0.45f -> Rational(9, 20)
                ratio > 2.35f -> Rational(235, 100)
                else -> Rational(width, height)
            }
        }
    }
}
