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

    /**
     * 바탕에 **색을 넣지 않습니다.** 사진이 주인공이라, 바탕이 누러면 사진의 흰색까지
     * 같이 누레 보입니다. 카드는 순백이고 바탕은 한 톤 낮아 층만 구분합니다 —
     * 나머지는 그림자가 맡습니다.
     */
    /**
     * ⚠️ 이 값의 **사본이 `app/src/main/res/values/colors.xml` 에 있습니다**
     * (`windowBackground`). XML 은 여기 상수를 못 읽습니다. 여기만 고치면 Compose 가
     * 그려지기 전에 깔리는 색이 옛 색으로 남아 앱이 통째로 그 색으로 보입니다.
     */
    val Paper = Color(0xFFFAFAFA)    // 화면 바탕
    val Surface = Color(0xFFFFFFFF)  // 떠 있는 면 — 순백
    val Fill = Color(0xFFF0F0F0)     // 보조 버튼 · 입력칸
    val Line = Color(0xFFE6E6E6)
    val Line2 = Color(0xFFD8D8D8)

    /** 지도에 넓게 깔리는 초록. 표지의 언덕과 같은 색입니다. */
    val Moss = Color(0xFF7FA98C)
    val MossSoft = Color(0xFFDCEBE0)
    val MossDeep = Color(0xFF2C5240)

    /** 그림 속 작은 것들. 강조색과 다투지 않게 아주 좁게 씁니다. */
    val Honey = Color(0xFFF0C46A)

    val MapSea = Color(0xFFDCEBE0)
    val MapLand = Color(0xFFEFE3CB)

    val OnAccent = Color(0xFFFFFFFF)
    /**
     * 시트 뒤를 어둡게 (32%). **색을 넣지 않습니다** — 화면 절반을 덮는 자리라
     * 조금만 물들여도 눈에 걸립니다. 먹색(따뜻한 회색) 그대로 씁니다.
     */
    val Scrim = Color(0x5235302A)

    /** 콘텐츠 위에 떠 있는 것에만 쓰는 반투명 흰색. 카드·시트에는 쓰지 않습니다. */
    val Glass = Color(0xD6FFFFFF)    // 84% 흰색
    val GlassBorder = Color(0x1235302A)
}
