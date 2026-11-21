package com.vela.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vela.player.data.downloader.DownloadOption
import com.vela.player.data.local.UserSettings
import com.vela.player.ui.components.VelaBadge
import com.vela.player.ui.components.VelaBadgeTone
import com.vela.player.ui.components.VelaChip
import com.vela.player.ui.components.VelaIcons
import com.vela.player.ui.components.VelaPanelCard
import com.vela.player.ui.components.VelaPrimaryButton
import com.vela.player.ui.components.VelaSecondaryButton
import com.vela.player.ui.theme.ThemeMode
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    isUpdatingEngine: Boolean,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectDefaultQuality: (DownloadOption) -> Unit,
    onOpenInstagramLogin: () -> Unit,
    onLogoutInstagram: () -> Unit,
    onUpdateEngine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography

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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(VelaSpacing.s4)) {
                Text(
                    text = "Settings",
                    style = typography.page,
                    color = colors.textPrimary
                )
                Text(
                    text = "Vela Emerald Slate Theme • On-Device Downloader • Instagram Session",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // Theme Mode (Light / Dark / System per Vela Master Guide)
        item {
            VelaPanelCard(
                header = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Appearance",
                            style = typography.cardTitle,
                            color = colors.textPrimary
                        )
                        VelaBadge(
                            label = "Emerald Slate",
                            tone = VelaBadgeTone.SUCCESS
                        )
                    }
                }
            ) {
                Text(
                    text = "Switch between Vela Dark (#09090B / #1E2230 / #10B981) and Light (#FAFAFA / #FFFFFF / #10B981).",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(VelaSpacing.s12))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8),
                    verticalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val icon = when (mode) {
                            ThemeMode.DARK -> VelaIcons.Moon
                            ThemeMode.LIGHT -> VelaIcons.Sun
                            ThemeMode.SYSTEM -> VelaIcons.Smartphone
                        }
                        VelaChip(
                            label = mode.label,
                            selected = settings.themeMode == mode,
                            iconRes = icon,
                            onClick = { onSelectTheme(mode) }
                        )
                    }
                }
            }
        }

        // Default Download Quality
        item {
            VelaPanelCard(
                header = {
                    Text(
                        text = "Default Download Quality",
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )
                }
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8),
                    verticalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
                ) {
                    listOf(
                        DownloadOption.VIDEO_360P,
                        DownloadOption.VIDEO_720P,
                        DownloadOption.VIDEO_1080P,
                        DownloadOption.AUDIO_ONLY
                    ).forEach { opt ->
                        VelaChip(
                            label = opt.label,
                            selected = settings.defaultQuality == opt.id,
                            onClick = { onSelectDefaultQuality(opt) }
                        )
                    }
                }
            }
        }

        // Instagram Authentication Session
        item {
            VelaPanelCard(
                header = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instagram Account",
                            style = typography.cardTitle,
                            color = colors.textPrimary
                        )
                        VelaBadge(
                            label = if (settings.igLoggedIn) "Connected" else "Not signed in",
                            tone = if (settings.igLoggedIn) VelaBadgeTone.SUCCESS else VelaBadgeTone.NEUTRAL,
                            iconRes = if (settings.igLoggedIn) VelaIcons.ShieldCheck else VelaIcons.Lock
                        )
                    }
                }
            ) {
                Text(
                    text = "Sign in to download Instagram Stories, private Reels, and restricted photo posts. Cookies are stored strictly in private on-device storage.",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(VelaSpacing.s16))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s12)
                ) {
                    VelaPrimaryButton(
                        text = if (settings.igLoggedIn) "Re-authenticate" else "Log in to Instagram",
                        iconRes = VelaIcons.LogIn,
                        onClick = onOpenInstagramLogin,
                        modifier = Modifier.weight(1f)
                    )
                    if (settings.igLoggedIn) {
                        VelaSecondaryButton(
                            text = "Log Out",
                            iconRes = VelaIcons.LogOut,
                            danger = true,
                            onClick = onLogoutInstagram
                        )
                    }
                }
            }
        }

        // On-Device yt-dlp Engine Maintenance
        item {
            VelaPanelCard(
                header = {
                    Text(
                        text = "Downloader Engine (yt-dlp)",
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )
                }
            ) {
                Text(
                    text = "If YouTube or Instagram changes its site format, update the on-device yt-dlp engine over-the-air without reinstalling VelaPlayer.",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(VelaSpacing.s16))
                VelaPrimaryButton(
                    text = if (isUpdatingEngine) "Updating Engine..." else "Update yt-dlp Engine",
                    iconRes = VelaIcons.Refresh,
                    loading = isUpdatingEngine,
                    onClick = onUpdateEngine,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
