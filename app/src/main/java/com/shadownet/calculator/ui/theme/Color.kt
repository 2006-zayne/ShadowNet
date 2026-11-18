package com.shadownet.calculator.ui.theme

import androidx.compose.ui.graphics.Color

// --- Casio Scientific Calculator Theme Palette ---

// Dark Theme Colors
val DarkFaceplateBg = Color(0xFF161A18)
val DarkScientificKeyBg = Color(0xFF222825)   // Upper scientific keys: dark slate charcoal
val DarkNumberKeyBg = Color(0xFF2C3431)      // Lower numeric keys: distinct warm charcoal
val DarkOperatorKeyBg = Color(0xFF333E3B)    // Operators & equals
val DarkKeyText = Color(0xFFF8FAFC)

// Light Theme Colors (Silver/Grey Professional Casio Style)
val LightFaceplateBg = Color(0xFFCBD5E1)     // Silver-gray calculator body
val LightScientificKeyBg = Color(0xFF475569) // Upper scientific keys: distinct slate gray
val LightScientificKeyText = Color(0xFFF8FAFC)
val LightNumberKeyBg = Color(0xFFF1F5F9)     // Lower numeric keys: light grey/white
val LightNumberKeyText = Color(0xFF0F172A)
val LightOperatorKeyBg = Color(0xFF64748B)   // Operators
val LightOperatorKeyText = Color(0xFFFFFFFF)

// Shared Function / Accent Keys
val FuncKeyBg = Color(0xFF1E2321)
val KeyTextLight = Color(0xFFF8FAFC)

val CasioLcdBg = Color(0xFFD4E2DC)
val CasioLcdText = Color(0xFF13221B)
val CasioLcdDarkBg = Color(0xFF151D19)
val CasioLcdDarkText = Color(0xFF34D399)

// Key category accents
val ShiftKeyBg = Color(0xFFEAB308)       // Yellow SHIFT
val AlphaKeyBg = Color(0xFFEC4899)       // Pink ALPHA
val DelKeyBg = Color(0xFF10B981)         // Green DEL
val AcKeyBg = Color(0xFF059669)          // Green AC
val EqualsKeyBg = Color(0xFF2563EB)      // Blue =

// Theme-aware Faceplate Secondary Label Colors
// Black in light mode for crisp contrast, yellow in dark mode.
fun getSecondaryLabelColor(isDark: Boolean): Color = if (isDark) Color(0xFFFACC15) else Color(0xFF0F172A)
fun getAlphaLabelColor(isDark: Boolean): Color = if (isDark) Color(0xFFF472B6) else Color(0xFF9D174D)

// Compatibility Aliases for Theme.kt
val DarkOperatorKeyContent = DarkKeyText
val DarkScientificKeyContent = DarkKeyText
val DarkNumberKeyContent = DarkKeyText
val LightScientificKeyContent = LightScientificKeyText
val LightNumberKeyContent = LightNumberKeyText
val LightOperatorKeyContent = LightOperatorKeyText

val LightBackground = Color(0xFFE2E8F0)
val LightSurface = Color(0xFFCBD5E1)
val LightDisplayBg = Color(0xFFD4E2DC)
val LightDisplayText = Color(0xFF13221B)
val LightDisplayPreview = Color(0xFF2C5E43)

val DarkBackground = Color(0xFF121514)
val DarkSurface = Color(0xFF191E1C)
val DarkDisplayBg = Color(0xFF151D19)
val DarkDisplayText = Color(0xFF34D399)
val DarkDisplayPreview = Color(0xFF10B981)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val ResultText = Color(0xFF34D399)
val ErrorText = Color(0xFFEF4444)
val InterceptedBannerBg = Color(0xFF7C3AED)
val InterceptedBannerContent = Color(0xFFFFFFFF)
