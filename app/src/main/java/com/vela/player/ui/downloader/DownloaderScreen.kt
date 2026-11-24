package com.vela.player.ui.downloader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vela.player.data.downloader.AnalyzedMedia
import com.vela.player.data.downloader.DownloadJob
import com.vela.player.data.downloader.DownloadOption
import com.vela.player.data.downloader.DownloadPlatform
import com.vela.player.data.downloader.DownloadStatus
import com.vela.player.ui.DownloaderUiState
import com.vela.player.ui.components.ColorOverride
import com.vela.player.ui.components.VelaBadge
import com.vela.player.ui.components.VelaBadgeTone
import com.vela.player.ui.components.VelaChip
import com.vela.player.ui.components.VelaIcon
import com.vela.player.ui.components.VelaIconButton
import com.vela.player.ui.components.VelaIcons
import com.vela.player.ui.components.VelaPanelCard
import com.vela.player.ui.components.VelaPrimaryButton
import com.vela.player.ui.components.VelaProductCard
import com.vela.player.ui.components.VelaSecondaryButton
import com.vela.player.ui.components.VelaTextField
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSize
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme
import com.vela.player.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloaderScreen(
    state: DownloaderUiState,
    jobs: List<DownloadJob>,
    engineReady: Boolean,
    engineError: String?,
    igLoggedIn: Boolean,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onSelectOption: (DownloadOption) -> Unit,
    onStartDownload: () -> Unit,
    onStreamNow: (AnalyzedMedia) -> Unit,
    onOpenInstagramLogin: () -> Unit,
    onCancelJob: (String) -> Unit,
    onDismissJob: (String) -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas),
        contentPadding = PaddingValues(
            start = VelaSpacing.s16,
            end = VelaSpacing.s16,
            top = VelaSpacing.s16,
            bottom = VelaSpacing.s32
        ),
        verticalArrangement = Arrangement.spacedBy(VelaSpacing.s16)
    ) {
        // Page Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(VelaSpacing.s8)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloader",
                        style = typography.page,
                        color = colors.textPrimary
                    )
                    VelaBadge(
                        label = when {
                            engineError != null -> "Engine error"
                            engineReady -> "On-Device Ready"
                            else -> "Starting engine..."
                        },
                        tone = when {
                            engineError != null -> VelaBadgeTone.DANGER
                            engineReady -> VelaBadgeTone.SUCCESS
                            else -> VelaBadgeTone.INFO
                        },
                        iconRes = if (engineReady) VelaIcons.Check else VelaIcons.Sparkles
                    )
                }
                Text(
                    text = "Paste or share a YouTube, Instagram or Dailymotion link. Dailymotion videos can be watched ad-free right here.",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // Unified URL Input Card
        item {
            VelaPanelCard(
                header = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YouTube, Instagram & Dailymotion Link",
                            style = typography.cardTitle,
                            color = colors.textPrimary
                        )
                        if (igLoggedIn) {
                            VelaBadge(
                                label = "IG Signed In",
                                tone = VelaBadgeTone.SUCCESS,
                                iconRes = VelaIcons.ShieldCheck
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VelaRadius.pill))
                                    .clickable(onClick = onOpenInstagramLogin)
                                    .padding(horizontal = VelaSpacing.s8, vertical = VelaSpacing.s4),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
                            ) {
                                VelaIcon(
                                    iconRes = VelaIcons.LogIn,
                                    contentDescription = null,
                                    size = 14.dp,
                                    tint = colors.linkColor
                                )
                                Text(
                                    text = "IG Login",
                                    style = typography.caption,
                                    color = colors.linkColor
                                )
                            }
                        }
                    }
                }
            ) {
                VelaTextField(
                    value = state.urlInput,
                    onValueChange = onUrlChange,
                    placeholder = "https://youtube.com/... or instagram.com/...",
                    leadingIconRes = VelaIcons.Link,
                    imeAction = ImeAction.Go,
                    onSubmit = onAnalyze,
                    trailingSlot = {
                        VelaIconButton(
                            iconRes = VelaIcons.Clipboard,
                            contentDescription = "Paste link from clipboard",
                            tint = ColorOverride.Emerald,
                            onClick = {
                                val pasted = clipboard.getText()?.text.orEmpty().trim()
                                if (pasted.isNotEmpty()) {
                                    onUrlChange(pasted)
                                }
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(VelaSpacing.s12))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s12)
                ) {
                    VelaPrimaryButton(
                        text = if (state.isAnalyzing) "Inspecting..." else "Fetch Media",
                        iconRes = VelaIcons.Download,
                        loading = state.isAnalyzing,
                        enabled = engineReady && state.urlInput.isNotBlank(),
                        onClick = onAnalyze,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Analyzed Media Preview + Quality Selector
        state.analyzedMedia?.let { analyzed ->
            item {
                AnalyzedMediaCard(
                    media = analyzed,
                    selectedOption = state.selectedOption,
                    onSelectOption = onSelectOption,
                    onStartDownload = onStartDownload,
                    onStreamNow = { onStreamNow(analyzed) },
                    onOpenInstagramLogin = onOpenInstagramLogin
                )
            }
        }

        // Active & Recent Download Queue
        if (jobs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloads (${jobs.size})",
                        style = typography.section,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Open Gallery",
                        style = typography.label,
                        color = colors.linkColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(VelaRadius.control))
                            .clickable(onClick = onOpenGallery)
                            .padding(horizontal = VelaSpacing.s8, vertical = VelaSpacing.s8)
                    )
                }
            }

            items(jobs, key = { it.id }) { job ->
                DownloadJobRow(
                    job = job,
                    onCancel = { onCancelJob(job.id) },
                    onDismiss = { onDismissJob(job.id) }
                )
            }
        } else {
            // Quick usage hint card
            item {
                VelaProductCard {
                    Text(
                        text = "Quick Tip — Share Directly",
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(VelaSpacing.s8))
                    Text(
                        text = "Tap Share in the YouTube, Instagram or Dailymotion app and choose VelaPlayer to inspect resolutions (360p, 720p, 1080p, Audio Only, or IG Photos) in one tap.",
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalyzedMediaCard(
    media: AnalyzedMedia,
    selectedOption: DownloadOption,
    onSelectOption: (DownloadOption) -> Unit,
    onStartDownload: () -> Unit,
    onStreamNow: () -> Unit,
    onOpenInstagramLogin: () -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography

    VelaProductCard(accentBorder = media.errorMessage == null && !media.requiresInstagramLogin) {
        // Error or Instagram Auth Required State
        if (media.requiresInstagramLogin) {
            VelaBadge(
                label = "Instagram Sign-In Needed",
                tone = VelaBadgeTone.WARNING,
                iconRes = VelaIcons.Lock
            )
            Spacer(modifier = Modifier.height(VelaSpacing.s12))
            Text(
                text = media.title,
                style = typography.section,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(VelaSpacing.s4))
            Text(
                text = media.errorMessage ?: "Log in with Instagram so VelaPlayer can fetch Stories or private media.",
                style = typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(VelaSpacing.s16))
            VelaPrimaryButton(
                text = "Open Instagram Login",
                iconRes = VelaIcons.LogIn,
                onClick = onOpenInstagramLogin,
                modifier = Modifier.fillMaxWidth()
            )
            return@VelaProductCard
        }

        if (media.errorMessage != null) {
            VelaBadge(
                label = "Unable to Load",
                tone = VelaBadgeTone.DANGER,
                iconRes = VelaIcons.AlertCircle
            )
            Spacer(modifier = Modifier.height(VelaSpacing.s8))
            Text(
                text = media.errorMessage,
                style = typography.bodySmall,
                color = colors.textSecondary
            )
            if (media.platform == DownloadPlatform.INSTAGRAM) {
                Spacer(modifier = Modifier.height(VelaSpacing.s12))
                VelaSecondaryButton(
                    text = "Try Signing In to Instagram",
                    iconRes = VelaIcons.LogIn,
                    onClick = onOpenInstagramLogin,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@VelaProductCard
        }

        // Preview Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(VelaRadius.control))
                    .background(colors.bgBase),
                contentAlignment = Alignment.Center
            ) {
                if (!media.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = media.thumbnailUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VelaIcon(
                        iconRes = if (media.imageUrls.isNotEmpty()) VelaIcons.Image else VelaIcons.Film,
                        contentDescription = null,
                        size = VelaSize.iconLg,
                        tint = colors.textMuted
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)) {
                    VelaBadge(
                        label = media.platform.label,
                        tone = when (media.platform) {
                            DownloadPlatform.YOUTUBE -> VelaBadgeTone.DANGER
                            DownloadPlatform.DAILYMOTION -> VelaBadgeTone.WARNING
                            else -> VelaBadgeTone.INFO
                        }
                    )
                    if (media.durationSec > 0L) {
                        VelaBadge(
                            label = FormatUtils.formatDurationSec(media.durationSec),
                            tone = VelaBadgeTone.NEUTRAL
                        )
                    } else if (media.imageUrls.size > 1) {
                        VelaBadge(
                            label = "${media.imageUrls.size} Photos",
                            tone = VelaBadgeTone.SUCCESS
                        )
                    }
                }
                Text(
                    text = media.title,
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!media.author.isNullOrBlank()) {
                    Text(
                        text = media.author,
                        style = typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(VelaSpacing.s16))

        Text(
            text = "Choose Format",
            style = typography.label,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(VelaSpacing.s8))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
        ) {
            media.availableOptions.forEach { option ->
                val icon = when {
                    option.isAudioOnly -> VelaIcons.Music
                    option.isImage -> VelaIcons.Image
                    else -> VelaIcons.Film
                }
                VelaChip(
                    label = option.label,
                    selected = selectedOption == option,
                    iconRes = icon,
                    onClick = { onSelectOption(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(VelaSpacing.s16))

        if (media.streamUrl != null) {
            VelaSecondaryButton(
                text = "Watch now · no ads",
                iconRes = VelaIcons.Play,
                onClick = onStreamNow,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(VelaSpacing.s8))
        }

        VelaPrimaryButton(
            text = "Save ${selectedOption.label}",
            iconRes = VelaIcons.Download,
            onClick = onStartDownload,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DownloadJobRow(
    job: DownloadJob,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val tone = when (job.status) {
        DownloadStatus.QUEUED -> VelaBadgeTone.NEUTRAL
        DownloadStatus.DOWNLOADING -> VelaBadgeTone.INFO
        DownloadStatus.MERGING -> VelaBadgeTone.WARNING
        DownloadStatus.COMPLETED -> VelaBadgeTone.SUCCESS
        DownloadStatus.FAILED -> VelaBadgeTone.DANGER
    }

    VelaPanelCard(contentPadding = PaddingValues(VelaSpacing.s16)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VelaBadge(label = job.status.label, tone = tone)
                    VelaBadge(label = job.option.label, tone = VelaBadgeTone.NEUTRAL)
                }
                Text(
                    text = job.title,
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = job.statusDetail,
                    style = typography.caption,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (job.status == DownloadStatus.DOWNLOADING || job.status == DownloadStatus.QUEUED) {
                VelaIconButton(
                    iconRes = VelaIcons.X,
                    contentDescription = "Cancel download",
                    tint = ColorOverride.Danger,
                    onClick = onCancel
                )
            } else {
                VelaIconButton(
                    iconRes = VelaIcons.X,
                    contentDescription = "Dismiss download entry",
                    tint = ColorOverride.Secondary,
                    onClick = onDismiss
                )
            }
        }

        if (job.status == DownloadStatus.DOWNLOADING || job.status == DownloadStatus.MERGING) {
            Spacer(modifier = Modifier.height(VelaSpacing.s12))
            LinearProgressIndicator(
                progress = { (job.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(VelaRadius.pill)),
                color = colors.brandPrimary,
                trackColor = colors.bgBase
            )
        }
    }
}
