package com.vela.player.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSize
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme
import com.vela.player.ui.theme.VelaToneColors

enum class VelaBadgeTone {
    NEUTRAL,
    INFO,
    WARNING,
    SUCCESS,
    DANGER
}

/**
 * Primary Emerald Action Button (Master Guide Section 2 & 8):
 * - Fill: #10B981 (#059669 pressed), Foreground: #09090B in BOTH themes.
 * - Touch target >= 48dp, radius 8dp.
 * - Loading state preserves accessible name and prevents duplicate clicks.
 */
@Composable
fun VelaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bg = when {
        !enabled && !loading -> colors.actionPrimaryBg.copy(alpha = 0.35f)
        isPressed -> colors.actionPrimaryPressed
        else -> colors.actionPrimaryBg
    }
    val shape = RoundedCornerShape(VelaRadius.control)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = VelaSize.minTouchTarget)
            .clip(shape)
            .background(bg)
            .clickable(
                enabled = enabled && !loading,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s12),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.actionPrimaryFg
            )
            Spacer(modifier = Modifier.width(VelaSpacing.s8))
        } else if (iconRes != null) {
            VelaIcon(
                iconRes = iconRes,
                contentDescription = null,
                size = VelaSize.iconMd,
                tint = colors.actionPrimaryFg
            )
            Spacer(modifier = Modifier.width(VelaSpacing.s8))
        }
        Text(
            text = text,
            style = typography.label,
            color = colors.actionPrimaryFg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Secondary / Ghost Surface Button with >=48dp touch height and contrast-checked border.
 */
@Composable
fun VelaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(VelaRadius.control)

    val bg = when {
        danger -> colors.statusDanger.bg
        isPressed -> colors.bgHover
        else -> colors.bgSurface
    }
    val borderColor = if (danger) colors.statusDanger.border else colors.borderInput
    val fg = if (danger) colors.statusDanger.fg else colors.textPrimary

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = VelaSize.minTouchTarget)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s12),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            VelaIcon(
                iconRes = iconRes,
                contentDescription = null,
                size = VelaSize.iconMd,
                tint = fg
            )
            Spacer(modifier = Modifier.width(VelaSpacing.s8))
        }
        Text(
            text = text,
            style = typography.label,
            color = if (enabled) fg else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Accessible Icon Action Button enforcing >=48dp touch target independent of glyph size.
 */
@Composable
fun VelaIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: ColorOverride = ColorOverride.Default,
    containerActive: Boolean = false,
    enabled: Boolean = true
) {
    val colors = VelaTheme.colors
    val shape = RoundedCornerShape(VelaRadius.control)
    val resolvedTint = when (tint) {
        ColorOverride.Default -> colors.textPrimary
        ColorOverride.Secondary -> colors.textSecondary
        ColorOverride.Emerald -> colors.brandPrimary
        ColorOverride.Star -> colors.starActive
        ColorOverride.Danger -> colors.statusDanger.fg
        ColorOverride.OnEmerald -> colors.actionPrimaryFg
    }
    val bg = if (containerActive) colors.brandPrimary.copy(alpha = 0.16f) else colors.bgSurface.copy(alpha = 0f)

    Box(
        modifier = modifier
            .size(VelaSize.minTouchTarget)
            .clip(shape)
            .background(bg)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        VelaIcon(
            iconRes = iconRes,
            contentDescription = null,
            size = VelaSize.iconMd,
            tint = if (enabled) resolvedTint else colors.textMuted
        )
    }
}

enum class ColorOverride {
    Default, Secondary, Emerald, Star, Danger, OnEmerald
}

/**
 * Semantic Status Badge (Master Guide Section 3):
 * 12sp caption label paired with an optional leading icon — never color alone.
 */
@Composable
fun VelaBadge(
    label: String,
    tone: VelaBadgeTone = VelaBadgeTone.NEUTRAL,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null
) {
    val colors = VelaTheme.colors
    val palette: VelaToneColors = when (tone) {
        VelaBadgeTone.NEUTRAL -> colors.statusNeutral
        VelaBadgeTone.INFO -> colors.statusInfo
        VelaBadgeTone.WARNING -> colors.statusWarning
        VelaBadgeTone.SUCCESS -> colors.statusSuccess
        VelaBadgeTone.DANGER -> colors.statusDanger
    }
    val shape = RoundedCornerShape(VelaRadius.pill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.bg)
            .border(BorderStroke(1.dp, palette.border), shape)
            .padding(horizontal = VelaSpacing.s8, vertical = VelaSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
    ) {
        if (iconRes != null) {
            VelaIcon(
                iconRes = iconRes,
                contentDescription = null,
                size = 12.dp,
                tint = palette.fg
            )
        }
        Text(
            text = label,
            style = VelaTheme.typography.caption,
            color = palette.fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Filter / Selection Chip with >=48dp touch height and readable active emerald state.
 */
@Composable
fun VelaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null
) {
    val colors = VelaTheme.colors
    val shape = RoundedCornerShape(VelaRadius.pill)
    val bg = if (selected) colors.actionPrimaryBg else colors.bgSurface
    val fg = if (selected) colors.actionPrimaryFg else colors.textPrimary
    val border = if (selected) colors.actionPrimaryBg else colors.borderInput

    Row(
        modifier = modifier
            .heightIn(min = VelaSize.minTouchTarget)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
    ) {
        if (iconRes != null) {
            VelaIcon(
                iconRes = iconRes,
                contentDescription = null,
                size = VelaSize.iconSm,
                tint = fg
            )
        }
        Text(
            text = label,
            style = VelaTheme.typography.label,
            color = fg,
            maxLines = 1
        )
    }
}

/**
 * Shared Search / URL Input Field (Master Guide Section 5):
 * - Input text >= 16sp (`typography.body`).
 * - Named clear action that returns focus to the input.
 * - Separate contrast-checked border.
 */
@Composable
fun VelaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconRes: Int? = VelaIcons.Search,
    onSubmit: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Search,
    trailingSlot: (@Composable () -> Unit)? = null
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(VelaRadius.control)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VelaSize.minTouchTarget)
            .clip(shape)
            .background(colors.bgBase)
            .border(BorderStroke(1.dp, colors.borderInput), shape)
            .padding(start = VelaSpacing.s12, end = VelaSpacing.s4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIconRes != null) {
            VelaIcon(
                iconRes = leadingIconRes,
                contentDescription = null,
                size = VelaSize.iconMd,
                tint = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(VelaSpacing.s8))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = VelaSpacing.s12),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = typography.body,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = typography.body.copy(color = colors.textPrimary),
                singleLine = true,
                cursorBrush = SolidColor(colors.brandPrimary),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onSearch = { onSubmit?.invoke() },
                    onDone = { onSubmit?.invoke() },
                    onGo = { onSubmit?.invoke() }
                )
            )
        }

        if (value.isNotEmpty()) {
            VelaIconButton(
                iconRes = VelaIcons.X,
                contentDescription = "Clear text",
                tint = ColorOverride.Secondary,
                onClick = {
                    onValueChange("")
                    focusRequester.requestFocus()
                }
            )
        }

        if (trailingSlot != null) {
            trailingSlot()
        }
    }
}
