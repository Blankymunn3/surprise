package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 색은 `docs/app/design.html` 이 원본입니다. 여기 없는 색을 화면에서 만들어 쓰지 않습니다.
 *
 * 강조색은 **하나**입니다. 두 번째 강조색을 만들면 무엇이 중요한지 알 수 없게 됩니다.
 */
object MemoryColors {
    /** 강조는 **좁게**. 오늘·대표사진·올리기처럼 딱 한 번 눌러야 하는 것에만. */
    val Accent = Color(0xFFE0764F)   // 감빛 — 종이·초록과 같은 온도
    val AccentTint = Color(0xFFFBE7DD)

    val Ink = Color(0xFF35302A)      // 검정이 아니라 따뜻한 먹색
    val Ink2 = Color(0xFF6E675C)
    val Ink3 = Color(0xFFA79E90)

    val Paper = Color(0xFFF5EFE1)    // 화면 바탕 — 따뜻한 종이
    val Surface = Color(0xFFFFFDF7)  // 떠 있는 면 — 흰색에 종이 한 방울
    val Fill = Color(0xFFEFE7D5)     // 보조 버튼 · 입력칸
    val Line = Color(0xFFE6DDCA)
    val Line2 = Color(0xFFDCD2BC)

    /** 지도에 넓게 깔리는 초록. 표지의 언덕과 같은 색입니다. */
    val Moss = Color(0xFF7FA98C)
    val MossSoft = Color(0xFFDCEBE0)
    val MossDeep = Color(0xFF2C5240)

    /** 그림 속 작은 것들. 강조색과 다투지 않게 아주 좁게 씁니다. */
    val Honey = Color(0xFFF0C46A)

    val MapSea = Color(0xFFDCEBE0)
    val MapLand = Color(0xFFEFE3CB)

    val OnAccent = Color(0xFFFFFFFF)
    val Scrim = Color(0x522C5240)    // 시트 뒤 어둡게 (32%)

    /** 콘텐츠 위에 떠 있는 것에만 쓰는 반투명 흰색. 카드·시트에는 쓰지 않습니다. */
    val Glass = Color(0xD6FFFDF7)    // 84% — 종이색이라 흰색보다 덜 튑니다
    val GlassBorder = Color(0x1235302A)
}
