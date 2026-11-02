package com.shuvo.SmartDrive.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Dynamic App Color Scheme supporting buttery-smooth 60fps Day/Night transitions.
 */
data class AppThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val accentTeal: Color,
    val accentIndigo: Color,
    val headerTop: Color,
    val headerBottom: Color,
    val searchBg: Color,
    val cardShadow: Color
)

val LocalAppColors = staticCompositionLocalOf {
    AppThemeColors(
        isDark = false,
        background = LightBackground,
        surface = LightSurface,
        card = LightCard,
        cardBorder = LightCardBorder,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textTertiary = LightTextTertiary,
        divider = LightDivider,
        accentTeal = AppleTeal,
        accentIndigo = AppleIndigo,
        headerTop = OceanHeaderTop,
        headerBottom = OceanHeaderBottom,
        searchBg = LightSearchBg,
        cardShadow = LightCardShadow
    )
}

val MaterialTheme.appColors: AppThemeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

@Composable
fun Drive75Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // 450ms buttery-smooth animated color interpolation across the entire UI
    val animDuration = 450
    val animEasing = FastOutSlowInEasing

    val animatedBackground by animateColorAsState(
        targetValue = if (darkTheme) DarkBackground else LightBackground,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "bg"
    )
    val animatedSurface by animateColorAsState(
        targetValue = if (darkTheme) DarkSurface else LightSurface,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "surface"
    )
    val animatedCard by animateColorAsState(
        targetValue = if (darkTheme) DarkCard else LightCard,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "card"
    )
    val animatedCardBorder by animateColorAsState(
        targetValue = if (darkTheme) DarkCardBorder else LightCardBorder,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "border"
    )
    val animatedTextPrimary by animateColorAsState(
        targetValue = if (darkTheme) DarkTextPrimary else LightTextPrimary,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "textPrimary"
    )
    val animatedTextSecondary by animateColorAsState(
        targetValue = if (darkTheme) DarkTextSecondary else LightTextSecondary,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "textSecondary"
    )
    val animatedTextTertiary by animateColorAsState(
        targetValue = if (darkTheme) DarkTextTertiary else LightTextTertiary,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "textTertiary"
    )
    val animatedDivider by animateColorAsState(
        targetValue = if (darkTheme) DarkDivider else LightDivider,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "divider"
    )
    val animatedSearchBg by animateColorAsState(
        targetValue = if (darkTheme) DarkSearchBg else LightSearchBg,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "searchBg"
    )
    val animatedCardShadow by animateColorAsState(
        targetValue = if (darkTheme) DarkCardShadow else LightCardShadow,
        animationSpec = tween(animDuration, easing = animEasing),
        label = "cardShadow"
    )

    val dynamicColors = AppThemeColors(
        isDark = darkTheme,
        background = animatedBackground,
        surface = animatedSurface,
        card = animatedCard,
        cardBorder = animatedCardBorder,
        textPrimary = animatedTextPrimary,
        textSecondary = animatedTextSecondary,
        textTertiary = animatedTextTertiary,
        divider = animatedDivider,
        accentTeal = AppleTeal,
        accentIndigo = AppleIndigo,
        headerTop = OceanHeaderTop,
        headerBottom = OceanHeaderBottom,
        searchBg = animatedSearchBg,
        cardShadow = animatedCardShadow
    )

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AppleTeal,
            onPrimary = Color.White,
            primaryContainer = AppleTealDark,
            onPrimaryContainer = Color.White,
            secondary = AppleIndigo,
            onSecondary = Color.White,
            background = animatedBackground,
            onBackground = animatedTextPrimary,
            surface = animatedSurface,
            onSurface = animatedTextPrimary,
            surfaceVariant = animatedCard,
            onSurfaceVariant = animatedTextSecondary,
            error = StatusDanger,
            onError = Color.White,
            outline = animatedCardBorder
        )
    } else {
        lightColorScheme(
            primary = AppleTeal,
            onPrimary = Color.White,
            primaryContainer = AppleTealSoft,
            onPrimaryContainer = OceanHeaderTop,
            secondary = AppleIndigo,
            onSecondary = Color.White,
            background = animatedBackground,
            onBackground = animatedTextPrimary,
            surface = animatedSurface,
            onSurface = animatedTextPrimary,
            surfaceVariant = animatedCard,
            onSurfaceVariant = animatedTextSecondary,
            error = StatusDanger,
            onError = Color.White,
            outline = animatedCardBorder
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Ocean navy status bar for the signature header
                window.statusBarColor = OceanHeaderTop.toArgb()
                window.navigationBarColor = animatedBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides dynamicColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Satisfying Apple-style Day/Night Shift toggle button.
 * Includes smooth 360° spring rotation, morphing sun/moon, and luminous glow.
 */
@Composable
fun DayNightSwitchButton(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDark) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF38BDF8) else Color(0xFFFBBF24),
        animationSpec = tween(400),
        label = "iconColor"
    )

    val buttonBg by animateColorAsState(
        targetValue = if (isDark) Color(0x331E293B) else Color(0x33FFFFFF),
        animationSpec = tween(400),
        label = "btnBg"
    )

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .border(
                1.dp,
                if (isDark) Color(0x4038BDF8) else Color(0x4DFFFFFF),
                CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .rotate(rotation)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
            contentDescription = if (isDark) "Switch to Day Mode" else "Switch to Night Mode",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
