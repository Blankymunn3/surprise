package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * **이 앱에 어두운 화면은 없습니다.** 폰이 다크 모드여도 밝게 둡니다 —
 * 웹에서 `color-scheme: only light` 로 막아둔 것과 같은 규칙입니다.
 * 그래서 darkColorScheme 을 만들지 않습니다. 만들어 두면 언젠가 누가 켭니다.
 *
 * 강제 다크(force-dark)는 `themes.xml` 의 `android:forceDarkAllowed=false` 로 끕니다.
 */
private val LightScheme = lightColorScheme(
    primary = MemoryColors.Accent,
    onPrimary = MemoryColors.OnAccent,
    primaryContainer = MemoryColors.Fill,
    onPrimaryContainer = MemoryColors.Accent,
    background = MemoryColors.Paper,
    onBackground = MemoryColors.Ink,
    surface = MemoryColors.Surface,
    onSurface = MemoryColors.Ink,
    surfaceVariant = MemoryColors.Fill,
    onSurfaceVariant = MemoryColors.Ink2,
    outline = MemoryColors.Line2,
    outlineVariant = MemoryColors.Line,
    scrim = MemoryColors.Scrim,
)

@Composable
fun MemoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = MemoryTypography,
        content = content,
    )
}
