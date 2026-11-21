package com.vela.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vela Unified Design Master Guide v3.0.0 — Preserved Emerald Slate 60:30:10 Tokens.
 *
 * Hierarchy:
 * - 60% Backgrounds: Dark #09090B / #0F1117 / #181B23 | Light #FAFAFA / #F4F4F5
 * - 30% Surfaces:    Dark #1E2230 / #252A3A / Zinc-900 #18181B | Light #FFFFFF
 * - 10% Accents:     Core Emerald #10B981 (hover #34D399, pressed #059669)
 */
object VelaPalette {
    // 60% Dark Dominant Backgrounds
    val CanvasDark = Color(0xFF09090B)
    val BaseDark = Color(0xFF0F1117)
    val SubtleDark = Color(0xFF181B23)

    // 30% Dark Secondary Surfaces
    val SlateSurfaceDark = Color(0xFF1E2230)
    val SlateHoverDark = Color(0xFF252A3A)
    val SidebarDark = Color(0xFF161922)
    val Zinc900 = Color(0xFF18181B)
    val Zinc800 = Color(0xFF27272A)
    val Zinc700 = Color(0xFF3F3F46)

    // Light Mode Zinc Hierarchy
    val CanvasLight = Color(0xFFFAFAFA)
    val BaseLight = Color(0xFFF4F4F5)
    val SurfaceLight = Color(0xFFFFFFFF)
    val Zinc200 = Color(0xFFE4E4E7)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc500 = Color(0xFF71717A)
    val Zinc600 = Color(0xFF52525B)

    // 10% Original Emerald Accent
    val Emerald400 = Color(0xFF34D399)
    val Emerald500 = Color(0xFF10B981)
    val Emerald600 = Color(0xFF059669)
    val Emerald700 = Color(0xFF047857)
    val EmeraldGlow = Color(0x5910B981) // rgba(16, 185, 129, 0.35)

    // Supporting Semantic Status Families
    val Sky400 = Color(0xFF38BDF8)
    val Sky600 = Color(0xFF0284C7)
    val Amber400 = Color(0xFFFBBF24)
    val Amber600 = Color(0xFFD97706)
    val Red400 = Color(0xFFF87171)
    val Red600 = Color(0xFFDC2626)
}

@Immutable
data class VelaToneColors(
    val bg: Color,
    val border: Color,
    val fg: Color
)

@Immutable
data class VelaColors(
    val isDark: Boolean,
    // 60% Backgrounds
    val bgCanvas: Color,
    val bgBase: Color,
    val bgSubtle: Color,
    // 30% Surfaces & Cards (paint opacity applied to surface fill only)
    val bgSurface: Color,
    val bgSurfaceGlass: Color,
    val bgHover: Color,
    val bgSidebar: Color,
    val cardProductBg: Color,
    val cardProductOpaqueBg: Color,
    val cardBentoBg: Color,
    val cardPanelBg: Color,
    val cardDashboardBg: Color,
    // Borders & Focus
    val borderSubtle: Color,
    val borderPanel: Color,
    val borderInput: Color,
    val focusRing: Color,
    // 10% Emerald Brand & Primary Action
    val brandPrimary: Color,
    val brandHover: Color,
    val brandPressed: Color,
    val brandGlow: Color,
    val actionPrimaryBg: Color,
    val actionPrimaryPressed: Color,
    val actionPrimaryFg: Color,
    val linkColor: Color,
    // Typography
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Semantic Status Tones
    val statusNeutral: VelaToneColors,
    val statusInfo: VelaToneColors,
    val statusWarning: VelaToneColors,
    val statusSuccess: VelaToneColors,
    val statusDanger: VelaToneColors,
    // Star / Favorite Accent
    val starActive: Color
)

val VelaDarkColors = VelaColors(
    isDark = true,
    bgCanvas = VelaPalette.CanvasDark,
    bgBase = VelaPalette.BaseDark,
    bgSubtle = VelaPalette.SubtleDark,
    bgSurface = VelaPalette.SlateSurfaceDark,
    bgSurfaceGlass = VelaPalette.SlateSurfaceDark.copy(alpha = 0.90f),
    bgHover = VelaPalette.SlateHoverDark,
    bgSidebar = VelaPalette.SidebarDark,
    cardProductBg = VelaPalette.Zinc900.copy(alpha = 0.40f),
    cardProductOpaqueBg = VelaPalette.Zinc900,
    cardBentoBg = VelaPalette.Zinc900,
    cardPanelBg = VelaPalette.Zinc900,
    cardDashboardBg = VelaPalette.Zinc900.copy(alpha = 0.80f),
    borderSubtle = Color.White.copy(alpha = 0.06f),
    borderPanel = Color.White.copy(alpha = 0.05f),
    borderInput = VelaPalette.Zinc700,
    focusRing = VelaPalette.Emerald500,
    brandPrimary = VelaPalette.Emerald500,
    brandHover = VelaPalette.Emerald400,
    brandPressed = VelaPalette.Emerald600,
    brandGlow = VelaPalette.EmeraldGlow,
    actionPrimaryBg = VelaPalette.Emerald500,
    actionPrimaryPressed = VelaPalette.Emerald600,
    actionPrimaryFg = VelaPalette.CanvasDark,
    linkColor = VelaPalette.Emerald400,
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    statusNeutral = VelaToneColors(
        bg = VelaPalette.Zinc800.copy(alpha = 0.85f),
        border = Color.White.copy(alpha = 0.10f),
        fg = Color(0xFFE4E4E7)
    ),
    statusInfo = VelaToneColors(
        bg = VelaPalette.Sky400.copy(alpha = 0.14f),
        border = VelaPalette.Sky400.copy(alpha = 0.30f),
        fg = VelaPalette.Sky400
    ),
    statusWarning = VelaToneColors(
        bg = VelaPalette.Amber400.copy(alpha = 0.14f),
        border = VelaPalette.Amber400.copy(alpha = 0.30f),
        fg = VelaPalette.Amber400
    ),
    statusSuccess = VelaToneColors(
        bg = VelaPalette.Emerald500.copy(alpha = 0.14f),
        border = VelaPalette.Emerald500.copy(alpha = 0.32f),
        fg = VelaPalette.Emerald400
    ),
    statusDanger = VelaToneColors(
        bg = VelaPalette.Red400.copy(alpha = 0.14f),
        border = VelaPalette.Red400.copy(alpha = 0.30f),
        fg = VelaPalette.Red400
    ),
    starActive = VelaPalette.Amber400
)

val VelaLightColors = VelaColors(
    isDark = false,
    bgCanvas = VelaPalette.CanvasLight,
    bgBase = VelaPalette.BaseLight,
    bgSubtle = VelaPalette.BaseLight,
    bgSurface = VelaPalette.SurfaceLight,
    bgSurfaceGlass = VelaPalette.SurfaceLight.copy(alpha = 0.94f),
    bgHover = VelaPalette.BaseLight,
    bgSidebar = VelaPalette.SurfaceLight,
    cardProductBg = VelaPalette.SurfaceLight,
    cardProductOpaqueBg = VelaPalette.SurfaceLight,
    cardBentoBg = VelaPalette.SurfaceLight,
    cardPanelBg = VelaPalette.SurfaceLight,
    cardDashboardBg = VelaPalette.SurfaceLight,
    borderSubtle = VelaPalette.Zinc200,
    borderPanel = VelaPalette.Zinc200,
    borderInput = VelaPalette.Zinc400,
    focusRing = VelaPalette.Emerald700,
    brandPrimary = VelaPalette.Emerald500,
    brandHover = VelaPalette.Emerald400,
    brandPressed = VelaPalette.Emerald600,
    brandGlow = VelaPalette.EmeraldGlow,
    actionPrimaryBg = VelaPalette.Emerald500,
    actionPrimaryPressed = VelaPalette.Emerald600,
    actionPrimaryFg = VelaPalette.CanvasDark,
    linkColor = VelaPalette.Emerald700,
    textPrimary = VelaPalette.CanvasDark,
    textSecondary = VelaPalette.Zinc600,
    textMuted = VelaPalette.Zinc500,
    statusNeutral = VelaToneColors(
        bg = VelaPalette.BaseLight,
        border = VelaPalette.Zinc300,
        fg = VelaPalette.Zinc600
    ),
    statusInfo = VelaToneColors(
        bg = VelaPalette.Sky600.copy(alpha = 0.10f),
        border = VelaPalette.Sky600.copy(alpha = 0.25f),
        fg = VelaPalette.Sky600
    ),
    statusWarning = VelaToneColors(
        bg = VelaPalette.Amber600.copy(alpha = 0.10f),
        border = VelaPalette.Amber600.copy(alpha = 0.28f),
        fg = Color(0xFFB45309)
    ),
    statusSuccess = VelaToneColors(
        bg = VelaPalette.Emerald600.copy(alpha = 0.10f),
        border = VelaPalette.Emerald600.copy(alpha = 0.28f),
        fg = VelaPalette.Emerald700
    ),
    statusDanger = VelaToneColors(
        bg = VelaPalette.Red600.copy(alpha = 0.10f),
        border = VelaPalette.Red600.copy(alpha = 0.25f),
        fg = VelaPalette.Red600
    ),
    starActive = VelaPalette.Amber600
)

/** 4dp Base Spacing Scale from Vela Master Guide Section 5 */
object VelaSpacing {
    val s0: Dp = 0.dp
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s20: Dp = 20.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val s40: Dp = 40.dp
    val s48: Dp = 48.dp
    val s64: Dp = 64.dp
}

/** Named Geometry Radii from Vela Master Guide Section 5 */
object VelaRadius {
    val control: Dp = 8.dp
    val card: Dp = 12.dp
    val panel: Dp = 12.dp
    val product: Dp = 16.dp
    val bento: Dp = 16.dp
    val dialog: Dp = 16.dp
    val pill: Dp = 999.dp
}

/** Control Heights & Minimum 48dp Touch Targets */
object VelaSize {
    val controlSm: Dp = 32.dp
    val controlMd: Dp = 40.dp
    val controlLg: Dp = 48.dp
    val minTouchTarget: Dp = 48.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
}

/** Motion Durations (ms) from Vela Master Guide Section 6 */
object VelaMotion {
    const val fastMs = 120
    const val mediumMs = 180
    const val standardMs = 240
    const val productCardMs = 500
    const val pressScale = 0.97f
}
