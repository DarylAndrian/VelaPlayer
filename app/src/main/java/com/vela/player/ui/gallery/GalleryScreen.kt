package com.vela.player.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.vela.player.data.local.MediaItemEntity
import com.vela.player.data.local.MediaType
import com.vela.player.ui.GalleryScopeFilter
import com.vela.player.ui.GalleryTypeFilter
import com.vela.player.ui.components.ColorOverride
import com.vela.player.ui.components.VelaBadge
import com.vela.player.ui.components.VelaBadgeTone
import com.vela.player.ui.components.VelaChip
import com.vela.player.ui.components.VelaIcon
import com.vela.player.ui.components.VelaIconButton
import com.vela.player.ui.components.VelaIcons
import com.vela.player.ui.components.VelaPrimaryButton
import com.vela.player.ui.components.VelaProductCard
import com.vela.player.ui.components.VelaSecondaryButton
import com.vela.player.ui.components.VelaTextField
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSize
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme
import com.vela.player.util.FormatUtils

@Composable
fun GalleryScreen(
    items: List<MediaItemEntity>,
    totalCount: Int,
    starredCount: Int,
    scopeFilter: GalleryScopeFilter,
    typeFilter: GalleryTypeFilter,
    searchQuery: String,
    isScanning: Boolean,
    onScopeChange: (GalleryScopeFilter) -> Unit,
    onTypeChange: (GalleryTypeFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onScanDevice: () -> Unit,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onToggleStar: (MediaItemEntity) -> Unit,
    onDeleteMedia: (MediaItemEntity) -> Unit,
    onGoToDownloader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    var previewImageItem by remember { mutableStateOf<MediaItemEntity?>(null) }

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
        // Page Header + Scan Device Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Offline Library",
                        style = typography.page,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$totalCount saved • $starredCount starred",
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                }
                VelaSecondaryButton(
                    text = if (isScanning) "Scanning..." else "Scan Device",
                    iconRes = VelaIcons.FolderOpen,
                    enabled = !isScanning,
                    onClick = onScanDevice
                )
            }
        }

        // All vs Starred Segmented Filter (core feature #3)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VelaRadius.card))
                    .background(colors.bgSurface)
                    .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(VelaRadius.card))
                    .padding(VelaSpacing.s4),
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
            ) {
                GalleryScopeTab(
                    label = "All ($totalCount)",
                    iconRes = VelaIcons.Film,
                    selected = scopeFilter == GalleryScopeFilter.ALL,
                    onClick = { onScopeChange(GalleryScopeFilter.ALL) },
                    modifier = Modifier.weight(1f)
                )
                GalleryScopeTab(
                    label = "Starred ($starredCount)",
                    iconRes = VelaIcons.Star,
                    selected = scopeFilter == GalleryScopeFilter.STARRED,
                    onClick = { onScopeChange(GalleryScopeFilter.STARRED) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search & Media Type Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(VelaSpacing.s12)) {
                VelaTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search offline videos, audio, or photos..."
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
                ) {
                    GalleryTypeFilter.entries.forEach { filter ->
                        val icon = when (filter) {
                            GalleryTypeFilter.ALL -> null
                            GalleryTypeFilter.VIDEO -> VelaIcons.Film
                            GalleryTypeFilter.AUDIO -> VelaIcons.Music
                            GalleryTypeFilter.IMAGE -> VelaIcons.Image
                        }
                        VelaChip(
                            label = filter.label,
                            selected = typeFilter == filter,
                            iconRes = icon,
                            onClick = { onTypeChange(filter) }
                        )
                    }
                }
            }
        }

        // Empty State
        if (items.isEmpty()) {
            item {
                VelaProductCard {
                    Text(
                        text = if (scopeFilter == GalleryScopeFilter.STARRED) {
                            "No starred media yet"
                        } else {
                            "Your offline gallery is empty"
                        },
                        style = typography.section,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(VelaSpacing.s8))
                    Text(
                        text = if (scopeFilter == GalleryScopeFilter.STARRED) {
                            "Tap the star icon on any video, audio track, or photo to keep your favorites right here."
                        } else {
                            "Download videos from YouTube & Instagram or scan offline videos already stored on your phone."
                        },
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(VelaSpacing.s16))
                    Row(horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s12)) {
                        if (scopeFilter == GalleryScopeFilter.STARRED) {
                            VelaSecondaryButton(
                                text = "Show All Media",
                                onClick = { onScopeChange(GalleryScopeFilter.ALL) }
                            )
                        } else {
                            VelaPrimaryButton(
                                text = "Download Media",
                                iconRes = VelaIcons.Download,
                                onClick = onGoToDownloader
                            )
                            VelaSecondaryButton(
                                text = "Scan Phone",
                                iconRes = VelaIcons.FolderOpen,
                                onClick = onScanDevice
                            )
                        }
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                MediaGalleryCard(
                    item = item,
                    onOpen = {
                        if (item.parsedMediaType == MediaType.IMAGE) {
                            previewImageItem = item
                        } else {
                            onPlayMedia(item)
                        }
                    },
                    onToggleStar = { onToggleStar(item) },
                    onDelete = { onDeleteMedia(item) }
                )
            }
        }
    }

    // Full-screen offline Image Viewer for downloaded Instagram photos
    previewImageItem?.let { imgItem ->
        ImagePreviewDialog(
            item = imgItem,
            onToggleStar = {
                onToggleStar(imgItem)
                previewImageItem = imgItem.copy(isStarred = !imgItem.isStarred)
            },
            onDismiss = { previewImageItem = null }
        )
    }
}

@Composable
private fun GalleryScopeTab(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VelaTheme.colors
    val shape = RoundedCornerShape(VelaRadius.control)
    val bg = if (selected) colors.actionPrimaryBg else Color.Transparent
    val fg = if (selected) colors.actionPrimaryFg else colors.textSecondary

    Row(
        modifier = modifier
            .height(VelaSize.minTouchTarget)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VelaIcon(
            iconRes = iconRes,
            contentDescription = null,
            size = VelaSize.iconSm,
            tint = fg
        )
        Spacer(modifier = Modifier.width(VelaSpacing.s8))
        Text(
            text = label,
            style = VelaTheme.typography.label,
            color = fg,
            maxLines = 1
        )
    }
}

/**
 * Product-style media card following Master Guide Section 4:
 * Primary open target is separate from sibling Star & Delete buttons (no nested clickable buttons).
 */
@Composable
private fun MediaGalleryCard(
    item: MediaItemEntity,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography

    VelaProductCard(
        useOpaqueFallback = true,
        accentBorder = item.isStarred,
        contentPadding = PaddingValues(VelaSpacing.s16)
    ) {
        // Primary clickable body
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VelaRadius.control))
                .clickable(onClick = onOpen),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(VelaRadius.control))
                    .background(colors.bgBase),
                contentAlignment = Alignment.Center
            ) {
                val thumbModel = item.thumbnailPath ?: item.filePath
                if (item.parsedMediaType != MediaType.AUDIO || !item.thumbnailPath.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbModel,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VelaIcon(
                        iconRes = VelaIcons.Music,
                        contentDescription = null,
                        size = VelaSize.iconLg,
                        tint = colors.brandPrimary
                    )
                }

                // Duration pill over bottom-right corner with opaque backdrop for contrast
                if (item.durationMs > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(VelaSpacing.s4)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC09090B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(item.durationMs),
                            style = typography.tabularTime,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
            ) {
                Text(
                    text = item.title,
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VelaBadge(
                        label = item.parsedSource.label,
                        tone = when (item.parsedMediaType) {
                            MediaType.VIDEO -> VelaBadgeTone.INFO
                            MediaType.AUDIO -> VelaBadgeTone.SUCCESS
                            MediaType.IMAGE -> VelaBadgeTone.WARNING
                        }
                    )
                    item.resolutionLabel?.takeIf { it.isNotBlank() }?.let { res ->
                        VelaBadge(label = res, tone = VelaBadgeTone.NEUTRAL)
                    }
                }

                val sizeText = FormatUtils.formatFileSize(item.fileSizeBytes)
                val resumeHint = if (item.lastPositionMs > 3_000L && item.durationMs > 0L) {
                    "Resume ${FormatUtils.formatDuration(item.lastPositionMs)}"
                } else {
                    null
                }
                val metaLine = listOfNotNull(sizeText.takeIf { it.isNotBlank() }, resumeHint)
                    .joinToString(" • ")
                if (metaLine.isNotEmpty()) {
                    Text(
                        text = metaLine,
                        style = typography.caption,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(VelaSpacing.s12))

        // Sibling Action Row (>=48dp touch targets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VelaSecondaryButton(
                text = if (item.parsedMediaType == MediaType.IMAGE) "View Photo" else "Play",
                iconRes = if (item.parsedMediaType == MediaType.IMAGE) VelaIcons.Maximize else VelaIcons.Play,
                onClick = onOpen
            )

            Row(horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)) {
                VelaIconButton(
                    iconRes = VelaIcons.Star,
                    contentDescription = if (item.isStarred) "Remove from starred" else "Add to starred",
                    tint = if (item.isStarred) ColorOverride.Star else ColorOverride.Secondary,
                    containerActive = item.isStarred,
                    onClick = onToggleStar
                )
                VelaIconButton(
                    iconRes = VelaIcons.Trash,
                    contentDescription = "Remove from library",
                    tint = ColorOverride.Secondary,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    item: MediaItemEntity,
    onToggleStar: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090B))
        ) {
            AsyncImage(
                model = item.filePath,
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0xAA09090B))
                    .padding(horizontal = VelaSpacing.s12, vertical = VelaSpacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VelaIconButton(
                    iconRes = VelaIcons.ArrowLeft,
                    contentDescription = "Close photo viewer",
                    tint = ColorOverride.Default,
                    onClick = onDismiss
                )
                Text(
                    text = item.title,
                    style = VelaTheme.typography.cardTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = VelaSpacing.s8)
                )
                VelaIconButton(
                    iconRes = VelaIcons.Star,
                    contentDescription = if (item.isStarred) "Unstar photo" else "Star photo",
                    tint = if (item.isStarred) ColorOverride.Star else ColorOverride.Secondary,
                    onClick = onToggleStar
                )
            }
        }
    }
}
