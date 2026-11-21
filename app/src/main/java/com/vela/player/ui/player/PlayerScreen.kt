package com.vela.player.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.vela.player.player.PlaybackQueueMode
import com.vela.player.player.PlayerPlaybackState
import com.vela.player.player.VelaPlayerController
import com.vela.player.ui.components.VelaBadge
import com.vela.player.ui.components.VelaBadgeTone
import com.vela.player.ui.components.VelaChip
import com.vela.player.ui.components.VelaIcon
import com.vela.player.ui.components.VelaIcons
import com.vela.player.ui.theme.VelaPalette
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSize
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme
import com.vela.player.util.FormatUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    controller: VelaPlayerController,
    playbackState: PlayerPlaybackState,
    title: String,
    isAudioOnly: Boolean,
    isStarred: Boolean,
    isInPipMode: Boolean,
    isScreenLocked: Boolean,
    queueMode: PlaybackQueueMode,
    upNextTitle: String?,
    onQueueModeChange: (PlaybackQueueMode) -> Unit,
    onScreenLockChange: (Boolean) -> Unit,
    onToggleStar: () -> Unit,
    onRotateScreen: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEnterPip: () -> Unit,
    onClose: () -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography

    var showControls by remember { mutableStateOf(true) }
    var showUnlockPill by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var seekHint by remember { mutableStateOf<String?>(null) }

    // Normal slider seek drag state (prevents live position updates from fighting user's thumb)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragMs by remember { mutableFloatStateOf(0f) }

    // Poll playback position smoothly while visible
    LaunchedEffect(controller) {
        while (true) {
            controller.pollPosition()
            delay(250L)
        }
    }

    // Auto-hide controls after 3.5s during active playback
    LaunchedEffect(showControls, playbackState.isPlaying, isScreenLocked, showSpeedSheet, showQueueSheet, isDraggingSlider) {
        if (showControls && playbackState.isPlaying && !isScreenLocked && !showSpeedSheet && !showQueueSheet && !isDraggingSlider && !isAudioOnly) {
            delay(3_500L)
            showControls = false
        }
    }

    LaunchedEffect(seekHint) {
        if (seekHint != null) {
            delay(700L)
            seekHint = null
        }
    }

    // Auto-hide YouTube-style "Unlock" pill after 2.5s when locked
    LaunchedEffect(showUnlockPill, isScreenLocked) {
        if (isScreenLocked && showUnlockPill) {
            delay(2_500L)
            showUnlockPill = false
        }
    }

    // Intercept system back button while Screen Lock is enabled
    BackHandler(enabled = isScreenLocked) {
        showUnlockPill = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Hardware-accelerated Media3 Video Surface (or Audio Cover for Sound-Only files)
        if (!isAudioOnly) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = controller.exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != controller.exoPlayer) {
                        view.player = controller.exoPlayer
                    }
                }
            )
        } else {
            // Audio-Only Offline Player Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VelaPalette.CanvasDark),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VelaSpacing.s16),
                    modifier = Modifier.padding(VelaSpacing.s32)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(VelaPalette.Emerald500.copy(alpha = 0.16f))
                            .border(1.dp, VelaPalette.Emerald500.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        VelaIcon(
                            iconRes = VelaIcons.Music,
                            contentDescription = null,
                            size = 40.dp,
                            tint = VelaPalette.Emerald400
                        )
                    }
                    if (!isInPipMode) {
                        Text(
                            text = title,
                            style = typography.section,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        VelaBadge(
                            label = "Offline Audio",
                            tone = VelaBadgeTone.SUCCESS,
                            iconRes = VelaIcons.Volume2
                        )
                    }
                }
            }
        }

        // In Picture-in-Picture mode, render pure uninterrupted video with zero overlay clutter
        if (isInPipMode) {
            return@Box
        }

        // 2. Buffering indicator
        if (playbackState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                color = VelaPalette.Emerald500,
                strokeWidth = 3.dp
            )
        }

        // 3. Touch Layer: either YouTube-Style Screen Lock Blocker OR normal tap-to-toggle controls
        if (isScreenLocked) {
            // Consumes ALL touches/gestures on the video surface so accidental touches never interrupt playback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showUnlockPill = !showUnlockPill }
                        )
                    }
            ) {
                AnimatedVisibility(
                    visible = showUnlockPill,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(bottom = VelaSpacing.s32)
                ) {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = VelaSize.minTouchTarget)
                            .clip(RoundedCornerShape(VelaRadius.pill))
                            .background(Color(0xE609090B))
                            .border(
                                BorderStroke(1.dp, VelaPalette.Emerald500.copy(alpha = 0.7f)),
                                RoundedCornerShape(VelaRadius.pill)
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    onScreenLockChange(false)
                                    showUnlockPill = false
                                    showControls = true
                                }
                            )
                            .padding(horizontal = VelaSpacing.s20, vertical = VelaSpacing.s12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
                    ) {
                        VelaIcon(
                            iconRes = VelaIcons.Unlock,
                            contentDescription = null,
                            size = VelaSize.iconMd,
                            tint = VelaPalette.Emerald400
                        )
                        Text(
                            text = "Screen Locked • Tap to Unlock",
                            style = typography.label,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Free space: one tap shows controls, two taps on a half seek 10 seconds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { offset ->
                                val backward = offset.x < size.width / 2f
                                if (backward) {
                                    controller.seekBy(-VelaPlayerController.SKIP_STEP_MS)
                                    seekHint = "Back ${VelaPlayerController.SKIP_STEP_SECONDS}s"
                                } else {
                                    controller.seekBy(VelaPlayerController.SKIP_STEP_MS)
                                    seekHint = "Forward ${VelaPlayerController.SKIP_STEP_SECONDS}s"
                                }
                            }
                        )
                    }
            )

            // 4. Full Player Controls Overlay
            AnimatedVisibility(
                visible = showControls || isAudioOnly,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xB809090B),
                                    Color(0x3309090B),
                                    Color(0xCC09090B)
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    val backward = offset.x < size.width / 2f
                                    if (backward) {
                                        controller.seekBy(-VelaPlayerController.SKIP_STEP_MS)
                                        seekHint = "Back ${VelaPlayerController.SKIP_STEP_SECONDS}s"
                                    } else {
                                        controller.seekBy(VelaPlayerController.SKIP_STEP_MS)
                                        seekHint = "Forward ${VelaPlayerController.SKIP_STEP_SECONDS}s"
                                    }
                                }
                            )
                        }
                        .safeDrawingPadding()
                ) {
                    // Top Bar: Back, Title, Star, Speed, Lock, PiP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = VelaSpacing.s12, vertical = VelaSpacing.s8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.ArrowLeft,
                            contentDescription = "Back to gallery",
                            onClick = onClose
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = VelaSpacing.s8)
                        ) {
                            Text(
                                text = title,
                                style = typography.cardTitle,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val queueHint = when {
                                playbackState.isAudioDisabledForUltraSpeed -> "10x Ultra Speed • Audio muted above 8x"
                                queueMode == PlaybackQueueMode.REPEAT_ONE -> "Repeating this video"
                                !upNextTitle.isNullOrBlank() -> "Up next · $upNextTitle"
                                else -> queueMode.detail
                            }
                            Text(
                                text = queueHint,
                                style = typography.caption,
                                color = VelaPalette.Emerald400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.Star,
                            contentDescription = if (isStarred) "Unstar video" else "Star video",
                            tint = if (isStarred) VelaPalette.Amber400 else Color.White,
                            onClick = onToggleStar
                        )
                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.RotateScreen,
                            contentDescription = "Rotate screen",
                            onClick = onRotateScreen
                        )
                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.Pip,
                            contentDescription = "Enter floating Picture-in-Picture",
                            onClick = onEnterPip
                        )
                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.Lock,
                            contentDescription = "Lock screen to prevent accidental touches",
                            onClick = {
                                onScreenLockChange(true)
                                showControls = false
                                showUnlockPill = true
                            }
                        )
                    }

                    // Center Transport Controls: -10s, Play/Pause, +10s
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QueueStepButton(
                            label = "Prev",
                            iconRes = VelaIcons.SkipBack,
                            contentDescription = "Previous video",
                            onClick = onPrevious
                        )
                        SkipSecondsButton(
                            seconds = VelaPlayerController.SKIP_STEP_SECONDS,
                            forward = false,
                            onClick = { controller.seekBy(-VelaPlayerController.SKIP_STEP_MS) }
                        )

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(VelaPalette.Emerald500)
                                .clickable(role = Role.Button, onClick = { controller.togglePlayPause() })
                                .semantics {
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            VelaIcon(
                                iconRes = if (playbackState.isPlaying) VelaIcons.Pause else VelaIcons.Play,
                                contentDescription = null,
                                size = 28.dp,
                                tint = VelaPalette.CanvasDark
                            )
                        }

                        SkipSecondsButton(
                            seconds = VelaPlayerController.SKIP_STEP_SECONDS,
                            forward = true,
                            onClick = { controller.seekBy(VelaPlayerController.SKIP_STEP_MS) }
                        )
                        QueueStepButton(
                            label = "Next",
                            iconRes = VelaIcons.SkipForward,
                            contentDescription = "Next video",
                            onClick = onNext
                        )
                    }

                    // Bottom Bar: Normal Seek Slider + Timestamps + Mute + Speed Pill
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s12)
                    ) {
                        val duration = playbackState.durationMs.coerceAtLeast(1L)
                        val displayedPosition = if (isDraggingSlider) {
                            sliderDragMs.toLong()
                        } else {
                            playbackState.currentPositionMs
                        }

                        // Normal position seek slider
                        Slider(
                            value = displayedPosition.toFloat().coerceIn(0f, duration.toFloat()),
                            onValueChange = { newVal ->
                                isDraggingSlider = true
                                sliderDragMs = newVal
                            },
                            onValueChangeFinished = {
                                controller.seekTo(sliderDragMs.toLong())
                                isDraggingSlider = false
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = VelaPalette.Emerald400,
                                activeTrackColor = VelaPalette.Emerald500,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${FormatUtils.formatDuration(displayedPosition)} / ${FormatUtils.formatDuration(playbackState.durationMs)}",
                                style = typography.tabularTime,
                                color = Color.White
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
                            ) {
                                PlayerOverlayIconButton(
                                    iconRes = if (playbackState.isMutedByUser || playbackState.isAudioDisabledForUltraSpeed) {
                                        VelaIcons.VolumeX
                                    } else {
                                        VelaIcons.Volume2
                                    },
                                    contentDescription = if (playbackState.isMutedByUser) "Unmute" else "Mute",
                                    onClick = { controller.toggleMute() }
                                )

                                val queueIcon = when (queueMode) {
                                    PlaybackQueueMode.IN_ORDER -> VelaIcons.Repeat
                                    PlaybackQueueMode.SHUFFLE -> VelaIcons.Shuffle
                                    PlaybackQueueMode.REPEAT_ONE -> VelaIcons.RepeatOne
                                }
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = VelaSize.minTouchTarget)
                                        .clip(RoundedCornerShape(VelaRadius.pill))
                                        .background(VelaPalette.Emerald500)
                                        .clickable(role = Role.Button, onClick = { showQueueSheet = true })
                                        .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s8),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
                                ) {
                                    VelaIcon(
                                        iconRes = queueIcon,
                                        contentDescription = null,
                                        size = VelaSize.iconSm,
                                        tint = VelaPalette.CanvasDark
                                    )
                                    Text(
                                        text = queueMode.label,
                                        style = typography.label,
                                        color = VelaPalette.CanvasDark
                                    )
                                }

                                // Playback Speed Pill (1x to 10x)
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = VelaSize.minTouchTarget)
                                        .clip(RoundedCornerShape(VelaRadius.pill))
                                        .background(
                                            if (playbackState.speed != 1.0f) {
                                                VelaPalette.Emerald500
                                            } else {
                                                Color(0x991E2230)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (playbackState.speed != 1.0f) VelaPalette.Emerald400 else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(VelaRadius.pill)
                                        )
                                        .clickable(role = Role.Button, onClick = { showSpeedSheet = true })
                                        .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s8),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
                                ) {
                                    val speedFg = if (playbackState.speed != 1.0f) VelaPalette.CanvasDark else Color.White
                                    VelaIcon(
                                        iconRes = VelaIcons.Gauge,
                                        contentDescription = null,
                                        size = VelaSize.iconSm,
                                        tint = speedFg
                                    )
                                    Text(
                                        text = FormatUtils.formatSpeedLabel(playbackState.speed),
                                        style = typography.label,
                                        color = speedFg
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        seekHint?.let { hint ->
            val backward = hint.startsWith("Back")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = VelaSpacing.s24),
                contentAlignment = if (backward) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Text(
                    text = hint,
                    style = typography.section,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(VelaRadius.pill))
                        .background(Color(0xCC09090B))
                        .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s8)
                )
            }
        }

        // 5. Playback Speed Selector Modal (1x, 1.25x, 1.5x, 1.75x, 2x, 4x, 5x, 10x)
        if (showSpeedSheet && !isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xA609090B))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showSpeedSheet = false }
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(VelaSpacing.s16)
                        .clip(RoundedCornerShape(VelaRadius.dialog))
                        .background(colors.bgSurface)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(VelaRadius.dialog))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(VelaSpacing.s20),
                    verticalArrangement = Arrangement.spacedBy(VelaSpacing.s12)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Playback Speed",
                                style = typography.section,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "10x automatically mutes audio for smooth ultra-fast scrubbing",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.X,
                            contentDescription = "Close speed selector",
                            tint = colors.textSecondary,
                            onClick = { showSpeedSheet = false }
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8),
                        verticalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
                    ) {
                        VelaPlayerController.SPEED_PRESETS.forEach { preset ->
                            VelaChip(
                                label = FormatUtils.formatSpeedLabel(preset),
                                selected = playbackState.speed == preset,
                                onClick = {
                                    controller.setSpeed(preset)
                                    showSpeedSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showQueueSheet && !isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xA609090B))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showQueueSheet = false }
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(VelaSpacing.s16)
                        .clip(RoundedCornerShape(VelaRadius.dialog))
                        .background(colors.bgSurface)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(VelaRadius.dialog))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(VelaSpacing.s20),
                    verticalArrangement = Arrangement.spacedBy(VelaSpacing.s12)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Up next",
                                style = typography.section,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Choose what plays when this video ends",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        PlayerOverlayIconButton(
                            iconRes = VelaIcons.X,
                            contentDescription = "Close queue options",
                            tint = colors.textSecondary,
                            onClick = { showQueueSheet = false }
                        )
                    }
                    PlaybackQueueMode.entries.forEach { mode ->
                        val icon = when (mode) {
                            PlaybackQueueMode.IN_ORDER -> VelaIcons.Repeat
                            PlaybackQueueMode.SHUFFLE -> VelaIcons.Shuffle
                            PlaybackQueueMode.REPEAT_ONE -> VelaIcons.RepeatOne
                        }
                        VelaChip(
                            label = mode.label,
                            selected = queueMode == mode,
                            iconRes = icon,
                            onClick = {
                                onQueueModeChange(mode)
                                showQueueSheet = false
                            }
                        )
                        Text(
                            text = mode.detail,
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueStepButton(
    label: String,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = VelaSize.minTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = VelaSpacing.s4, vertical = VelaSpacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VelaIcon(
            iconRes = iconRes,
            contentDescription = null,
            size = 22.dp,
            tint = Color.White
        )
        Text(
            text = label,
            style = VelaTheme.typography.caption,
            color = Color.White
        )
    }
}

@Composable
private fun SkipSecondsButton(
    seconds: Int,
    forward: Boolean,
    onClick: () -> Unit
) {
    val label = if (forward) "Forward $seconds seconds" else "Back $seconds seconds"
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = VelaSize.minTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = VelaSpacing.s8, vertical = VelaSpacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VelaIcon(
            iconRes = if (forward) VelaIcons.RotateCw else VelaIcons.RotateCcw,
            contentDescription = null,
            size = 28.dp,
            tint = Color.White
        )
        Text(
            text = "${seconds}s",
            style = VelaTheme.typography.label,
            color = Color.White
        )
    }
}

@Composable
private fun PlayerOverlayIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(VelaSize.minTouchTarget)
            .clip(CircleShape)
            .background(Color(0x5509090B))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        VelaIcon(
            iconRes = iconRes,
            contentDescription = null,
            size = VelaSize.iconMd,
            tint = tint
        )
    }
}
