package com.vela.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vela.player.ui.theme.VelaMotion
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme

/**
 * Product Card recipe (Master Guide Section 4):
 * - 16dp radius, 20dp padding (overridable when needed)
 * - Dark: Zinc-900 @ 40% over known canvas (opaque fallback over imagery), White @ 6% border
 * - Light: Solid #FFFFFF, Zinc-200 border
 * - Opacity applies to background paint only — never to foreground text/controls.
 * - Press scale 0.97f when actionable and reduced motion is off.
 */
@Composable
fun VelaProductCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    useOpaqueFallback: Boolean = false,
    accentBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(VelaSpacing.s20),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = VelaTheme.colors
    val reducedMotion = VelaTheme.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed && !reducedMotion) VelaMotion.pressScale else 1f,
        animationSpec = tween(durationMillis = VelaMotion.fastMs),
        label = "VelaProductCardScale"
    )

    val shape = RoundedCornerShape(VelaRadius.product)
    val bgColor = if (useOpaqueFallback) colors.cardProductOpaqueBg else colors.cardProductBg
    val borderColor = when {
        accentBorder -> colors.brandPrimary.copy(alpha = 0.55f)
        isPressed && onClick != null -> colors.brandPrimary.copy(alpha = 0.45f)
        else -> colors.borderSubtle
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Bento Card recipe (Master Guide Section 4):
 * - 16dp radius, 24dp padding, solid white / zinc-900, quiet border, static by default.
 */
@Composable
fun VelaBentoCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(VelaSpacing.s24),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = VelaTheme.colors
    val shape = RoundedCornerShape(VelaRadius.bento)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.cardBentoBg)
            .border(BorderStroke(1.dp, colors.borderSubtle), shape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Panel Card recipe (Master Guide Section 4):
 * - 12dp radius, 20dp padding, solid white / zinc-900, white 5% dark edge, optional header divider.
 */
@Composable
fun VelaPanelCard(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(VelaSpacing.s20),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = VelaTheme.colors
    val shape = RoundedCornerShape(VelaRadius.panel)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.cardPanelBg)
            .border(BorderStroke(1.dp, colors.borderPanel), shape)
    ) {
        if (header != null) {
            Box(modifier = Modifier.padding(horizontal = VelaSpacing.s20, vertical = VelaSpacing.s16)) {
                header()
            }
            HorizontalDivider(thickness = 1.dp, color = colors.borderPanel)
        }
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Emerald Slate Surface Card recipe (#1E2230 @ 90% in dark, #FFFFFF in light):
 * - 12dp radius, 20dp padding.
 */
@Composable
fun VelaSurfaceCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(VelaSpacing.s20),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = VelaTheme.colors
    val shape = RoundedCornerShape(VelaRadius.card)
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor ?: colors.bgSurfaceGlass)
            .border(BorderStroke(1.dp, colors.borderSubtle), shape)
            .padding(contentPadding),
        content = content
    )
}
