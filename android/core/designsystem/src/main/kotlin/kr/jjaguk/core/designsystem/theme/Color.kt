package kr.jjaguk.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 색은 `docs/app/design.html` 이 원본입니다. 여기 없는 색을 화면에서 만들어 쓰지 않습니다.
 *
 * **UI 는 이 여섯이 전부입니다** — 바탕 · 표면 · 잉크 · 레드 · 딥레드 · 회색 글.
 * 색은 사진이 냅니다. 사진과 경쟁하는 유채색 UI 를 두지 않습니다.
 */
object MemoryColors {
    /** 주 동작 · 대표 · 오늘 · 에러. **이 넷 말고는 안 씁니다.** */
    val Accent = Color(0xFFEC3013)
    /** 작은 강조 글씨. 레드는 작게 쓰면 눈에 튀어서 한 단계 어둡게 갑니다. */
    val AccentDeep = Color(0xFFAE1800)

    val Ink = Color(0xFF201E1D)      // 글 · 선 · 탭
    val Ink2 = Color(0xFF7D7979)     // 메타
    val Ink3 = Color(0xFF9B9797)     // '아직 없음' 처럼 더 흐린 것

    /**
     * ⚠️ [Paper] 의 **사본이 `app/src/main/res/values/colors.xml` 에 있습니다**
     * (`windowBackground`). XML 은 여기 상수를 못 읽습니다. 여기만 고치면 Compose 가
     * 그려지기 전에 깔리는 색이 옛 색으로 남아 앱이 통째로 그 색으로 보입니다.
     */
    val Paper = Color(0xFFF3F2F2)    // 화면 바탕 — 웜 그레이
    val Surface = Color(0xFFFFFFFF)  // 떠 있는 면 — 흰 면
    val Fill = Color(0xFFEAE9E9)     // 그룹 헤더 · 눌린 자리

    /** 테두리 1px 은 **잉크 그대로**입니다. 흐린 회색 선을 쓰지 않습니다. */
    val Line = Ink
    /** 구획선 2px — 잉크 40%. */
    val Line2 = Color(0x66201E1D)

    val OnAccent = Color(0xFFFFFFFF)

    /**
     * 시트 뒤를 어둡게. 잉크를 그대로 묽혀 씁니다 — 화면 절반을 덮는 자리라
     * 색을 조금만 넣어도 눈에 걸립니다.
     */
    val Scrim = Color(0x52201E1D)

    /**
     * 지도에서 아직 안 다녀온 지역. **사진이 있는 지역만 사진으로 칠해지고**
     * 나머지는 이 회색입니다 — 지도 자체가 빈 화면의 그림 역할을 합니다.
     */
    val MapLand = Color(0xFFEAE9E9)

    /**
     * 바다. **여섯 색 밖의 유일한 예외입니다.**
     *
     * 지도는 UI 가 아니라 콘텐츠라, 사진과 마찬가지로 제 색을 냅니다. 땅과 바다가
     * 둘 다 회색이면 어디까지가 뭍인지 알 수 없어서, 바다에만 푸른 기를 남깁니다.
     */
    val MapSea = Color(0xFFE3E9EC)
}
