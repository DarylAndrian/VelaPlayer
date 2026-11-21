package com.vela.player.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.vela.player.R
import com.vela.player.ui.theme.VelaSize
import com.vela.player.ui.theme.VelaTheme

/**
 * Centralized Lucide icon references (from com.composables:icons-lucide-android:2.2.1).
 * R8 strips all unused drawable resources automatically in release builds.
 */
object VelaIcons {
    @DrawableRes val Download = R.drawable.lucide_ic_download
    @DrawableRes val Play = R.drawable.lucide_ic_play
    @DrawableRes val Pause = R.drawable.lucide_ic_pause
    @DrawableRes val Star = R.drawable.lucide_ic_star
    @DrawableRes val Lock = R.drawable.lucide_ic_lock
    @DrawableRes val Unlock = R.drawable.lucide_ic_lock_open
    @DrawableRes val Pip = R.drawable.lucide_ic_picture_in_picture_2
    @DrawableRes val Gauge = R.drawable.lucide_ic_gauge
    @DrawableRes val Film = R.drawable.lucide_ic_film
    @DrawableRes val Music = R.drawable.lucide_ic_music
    @DrawableRes val Image = R.drawable.lucide_ic_image
    @DrawableRes val Link = R.drawable.lucide_ic_link
    @DrawableRes val Clipboard = R.drawable.lucide_ic_clipboard_paste
    @DrawableRes val Search = R.drawable.lucide_ic_search
    @DrawableRes val X = R.drawable.lucide_ic_x
    @DrawableRes val Settings = R.drawable.lucide_ic_settings
    @DrawableRes val Sun = R.drawable.lucide_ic_sun
    @DrawableRes val Moon = R.drawable.lucide_ic_moon
    @DrawableRes val Smartphone = R.drawable.lucide_ic_smartphone
    @DrawableRes val Check = R.drawable.lucide_ic_check
    @DrawableRes val AlertCircle = R.drawable.lucide_ic_circle_alert
    @DrawableRes val Trash = R.drawable.lucide_ic_trash_2
    @DrawableRes val Refresh = R.drawable.lucide_ic_refresh_cw
    @DrawableRes val FolderOpen = R.drawable.lucide_ic_folder_open
    @DrawableRes val ArrowLeft = R.drawable.lucide_ic_arrow_left
    @DrawableRes val RotateCcw = R.drawable.lucide_ic_rotate_ccw
    @DrawableRes val RotateCw = R.drawable.lucide_ic_rotate_cw
    @DrawableRes val RotateScreen = R.drawable.lucide_ic_rotate_cw_square
    @DrawableRes val Shuffle = R.drawable.lucide_ic_shuffle
    @DrawableRes val Repeat = R.drawable.lucide_ic_repeat
    @DrawableRes val RepeatOne = R.drawable.lucide_ic_repeat_1
    @DrawableRes val SkipBack = R.drawable.lucide_ic_skip_back
    @DrawableRes val SkipForward = R.drawable.lucide_ic_skip_forward
    @DrawableRes val Volume2 = R.drawable.lucide_ic_volume_2
    @DrawableRes val VolumeX = R.drawable.lucide_ic_volume_x
    @DrawableRes val LogIn = R.drawable.lucide_ic_log_in
    @DrawableRes val LogOut = R.drawable.lucide_ic_log_out
    @DrawableRes val ShieldCheck = R.drawable.lucide_ic_shield_check
    @DrawableRes val Maximize = R.drawable.lucide_ic_maximize
    @DrawableRes val Sparkles = R.drawable.lucide_ic_sparkles
}

@Composable
fun VelaIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = VelaSize.iconMd,
    tint: Color = VelaTheme.colors.textPrimary
) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}
