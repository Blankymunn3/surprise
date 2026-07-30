package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 색은 `docs/app/design.html` 이 원본입니다. 여기 없는 색을 화면에서 만들어 쓰지 않습니다.
 *
 * 강조색은 **하나**입니다. 두 번째 강조색을 만들면 무엇이 중요한지 알 수 없게 됩니다.
 */
object MemoryColors {
    val Accent = Color(0xFFE11D5B)
    val AccentTint = Color(0xFFFDEDF2)

    val Ink = Color(0xFF1A1416)      // 검정이 아니라 따뜻한 먹색
    val Ink2 = Color(0xFF6E6266)
    val Ink3 = Color(0xFFA2969A)

    val Paper = Color(0xFFFBF8F9)    // 흰색에 로즈를 한 방울
    val Surface = Color(0xFFFFFFFF)
    val Fill = Color(0xFFF4EFF1)     // 빈 날짜 칸 · 보조 버튼
    val Line = Color(0xFFEDE5E8)
    val Line2 = Color(0xFFE0D5DA)

    val MapSea = Color(0xFFDEEAEF)
    val MapLand = Color(0xFFEFEAE3)

    val OnAccent = Color(0xFFFFFFFF)
    val Scrim = Color(0x52140C10)    // 시트 뒤 어둡게 (32%)

    /** 콘텐츠 위에 떠 있는 것에만 쓰는 반투명 흰색. 카드·시트에는 쓰지 않습니다. */
    val Glass = Color(0xB8FFFFFF)    // 72%
    val GlassBorder = Color(0x121A1416)
}
