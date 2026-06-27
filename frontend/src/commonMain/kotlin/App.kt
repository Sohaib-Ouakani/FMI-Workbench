import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import screens.HomeScreen

// ── Light engineering colour tokens ───────────────────────────────────────
val BgDeep = Color(0xFFF0F4F8) // page background — cool off-white
val BgSurface = Color(0xFFFFFFFF) // card / surface
val BgElevated = Color(0xFFE8EEF5) // elevated / inset areas
val AccentCyan = Color(0xFF0077AA) // primary accent — deep engineering blue
val AccentGreen = Color(0xFF007A3D) // success
val AccentAmber = Color(0xFFB36200) // warning
val AccentRed = Color(0xFFCC2200) // error
val TextPrimary = Color(0xFF0D1B2A) // main text
val TextSecondary = Color(0xFF4A6580) // muted text
val BorderSubtle = Color(0xFFBDCCDA) // dividers / borders

val EngineeringColorScheme = lightColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCE8F4),
    onPrimaryContainer = AccentCyan,
    secondary = AccentGreen,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBBF0D4),
    onSecondaryContainer = AccentGreen,
    tertiary = AccentAmber,
    background = BgDeep,
    surface = BgSurface,
    surfaceVariant = BgElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = Color(0xFFD8E4EE),
    error = AccentRed,
    onError = Color(0xFFFFFFFF),
)

val EngineeringTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 1.5.sp,
        color = AccentCyan,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.8.sp,
        color = TextSecondary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = TextPrimary,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = TextSecondary,
    ),
)

@Composable
fun App2() {
    MaterialTheme(
        colorScheme = EngineeringColorScheme,
        typography = EngineeringTypography,
    ) {
        Navigator(HomeScreen())
    }
}
