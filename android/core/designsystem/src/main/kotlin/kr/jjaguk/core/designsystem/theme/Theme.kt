package kr.jjaguk.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
        colorScheme = PlasticScheme,
        typography = MemoryTypography,
        content = content,
    )
}
