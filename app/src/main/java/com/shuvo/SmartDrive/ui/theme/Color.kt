package com.shuvo.SmartDrive.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// iOS Oceanic & Mint Aesthetic Palette
// Inspired by Apple iOS Files & Cloud Design
// ==========================================

// --- Signature Deep Oceanic Header Gradient ---
// Persistent across both Light and Dark mode for the iconic header look
val OceanHeaderTop = Color(0xFF0F1E36)          // Rich deep midnight navy
val OceanHeaderMid = Color(0xFF162D50)          // Midnight ocean blue
val OceanHeaderBottom = Color(0xFF1B3B6F)       // Deep vibrant oceanic blue
val OceanGlassCard = Color(0x38102A4C)          // Translucent frosted glass card
val OceanGlassBorder = Color(0x2EFFFFFF)        // Frosted white glass border

// --- Primary Brand & Action Colors ---
val AppleTeal = Color(0xFF2CD5C4)               // Iconic turquoise / aqua accent
val AppleTealDark = Color(0xFF1BBDAA)           // Deeper teal for active states
val AppleTealGlow = Color(0x592CD5C4)           // Diffuse luminous glow for shadows
val AppleTealSoft = Color(0x1F2CD5C4)           // Soft translucent teal tint

val AppleIndigo = Color(0xFF5B67F6)             // iOS Indigo accent
val AppleIndigoDark = Color(0xFF434EC9)
val AppleIndigoLight = Color(0xFFEEF2FF)
val AppleIndigoSoft = Color(0x265B67F6)

// --- Light Mode Palette (Crisp iOS Cloud & White) ---
val LightBackground = Color(0xFFF6F8FC)         // Soft crisp iOS background
val LightSurface = Color(0xFFFFFFFF)            // Pure white surface
val LightCard = Color(0xFFFFFFFF)               // Pure white card
val LightCardBorder = Color(0xFFE4E9F2)         // Soft subtle border
val LightTextPrimary = Color(0xFF0F172A)        // Deep ink text
val LightTextSecondary = Color(0xFF64748B)      // Slate gray secondary text
val LightTextTertiary = Color(0xFF94A3B8)       // Soft tertiary text
val LightDivider = Color(0xFFEDF2F7)            // Gentle divider line
val LightSearchBg = Color(0xFF1A3052)           // Frosted navy for header search
val LightCardShadow = Color(0x0F0F172A)         // Diffuse iOS card shadow

// --- Dark Mode Palette (Midnight Slate & Obsidian) ---
val DarkBackground = Color(0xFF0B111E)          // Deep obsidian dark background
val DarkSurface = Color(0xFF131D31)             // Dark navy slate surface
val DarkCard = Color(0xFF18233C)                // Elevated midnight card
val DarkCardBorder = Color(0xFF24324F)          // Midnight border
val DarkTextPrimary = Color(0xFFF8FAFC)         // Pure crisp white text
val DarkTextSecondary = Color(0xFF94A3B8)       // Muted slate text
val DarkTextTertiary = Color(0xFF64748B)        // Deep slate text
val DarkDivider = Color(0xFF1F2D47)             // Dark divider line
val DarkSearchBg = Color(0xFF152642)            // Search background
val DarkCardShadow = Color(0x33000000)          // Dark drop shadow

// --- Functional Status Colors ---
val StatusSuccess = Color(0xFF10B981)           // Emerald green
val StatusWarning = Color(0xFFF59E0B)           // Warm amber
val StatusDanger = Color(0xFFEF4444)            // Coral red
val StatusInfo = Color(0xFF0EA5E9)              // Sky blue

// --- Category Colors & Gradients ---
val CategoryPhoto = Color(0xFF2CD5C4)           // Mint / Aqua Teal
val CategoryVideo = Color(0xFFF43F5E)           // Rose Magenta
val CategoryDocument = Color(0xFFF59E0B)        // Solar Amber
val CategoryTextCode = Color(0xFF38BDF8)        // Sky Blue
val CategoryAudioOther = Color(0xFFA855F7)      // Lavender Purple

val CatImageGradStart = Color(0xFF38BDF8)
val CatImageGradEnd = Color(0xFF0284C7)
val CatVideoGradStart = Color(0xFFF43F5E)
val CatVideoGradEnd = Color(0xFFBE123C)
val CatDocGradStart = Color(0xFFF59E0B)
val CatDocGradEnd = Color(0xFFD97706)
val CatAudioGradStart = Color(0xFFA855F7)
val CatAudioGradEnd = Color(0xFF7E22CE)
val CatOtherGradStart = Color(0xFF10B981)
val CatOtherGradEnd = Color(0xFF059669)

// Backwards-compatibility aliases for existing references
val DiscordBlurple = AppleTeal
val DiscordBlurpleDark = AppleTealDark
val DiscordBlurpleLight = AppleTeal
val DiscordDarkBackground = DarkBackground
val DiscordDarkSurface = DarkSurface
val DiscordDarkCard = DarkCard
val DiscordDarkElevated = DarkSurface
val DiscordDarkContainer = DarkCard
val TextWhitePrimary = Color(0xFFF8FAFC)
val TextMutedSecondary = Color(0xFF94A3B8)
val TextDarkTertiary = Color(0xFF64748B)
val DividerColor = DarkDivider
val DangerRed = StatusDanger
val SuccessGreen = StatusSuccess
val WarningYellow = StatusWarning
