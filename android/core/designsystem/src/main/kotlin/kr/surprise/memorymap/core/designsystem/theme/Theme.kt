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

/**
 * 패미컴 스타일의 머티리얼 색.
 *
 * 우리가 직접 그리지 않는 것들이 여기서 색을 받아 갑니다 — **날짜 고르기 달력**,
 * 누를 때 번지는 물결(ripple), 글자 고를 때의 손잡이와 칠. 이걸 안 맞추면 앱은
 * 회색 플라스틱인데 달력만 혼자 보라색으로 뜹니다.
 *
 * `background`·`surface` 는 **몸통 색**입니다. 머티리얼이 알아서 무언가를 깔 때
 * 그것이 기기 위에 놓인 것처럼 보여야 합니다.
 */
private val PlasticScheme = lightColorScheme(
    primary = PlasticColors.Red,
    onPrimary = PlasticColors.OnRed,
    primaryContainer = PlasticColors.Trim,
    onPrimaryContainer = PlasticColors.Ink,
    background = PlasticColors.Body,
    onBackground = PlasticColors.Ink,
    surface = PlasticColors.Body,
    onSurface = PlasticColors.Ink,
    surfaceVariant = PlasticColors.Trim,
    onSurfaceVariant = PlasticColors.Ink,
    outline = PlasticColors.TrimLo,
    outlineVariant = PlasticColors.BodyLo,
    scrim = MemoryColors.Scrim,
)

@Composable
fun MemoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (PLASTIC_TRIAL) PlasticScheme else LightScheme,
        typography = MemoryTypography,
        content = content,
    )
}
