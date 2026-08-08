package kr.jjaguk.feature.map

import android.content.Context

/**
 * 지도 스타일. **두 겹**입니다:
 *
 * ```
 * 라벨   ← CARTO 벡터 타일 + 글리프 (지명·도로명이 화면 밀도와 무관하게 또렷)
 * 바탕   ← CARTO light_nolabels 를 TileProxy 가 픽셀화한 것
 * ```
 *
 * 라벨을 **벡터로 그리는 이유**: 라스터 라벨 타일은 고밀도 화면에서 흐릿하고
 * 글자가 작아 실기기에서 읽기 힘들었습니다. iOS 는 애플의 벡터 라벨을 쓰므로,
 * 안드로이드도 벡터로 그려야 두 앱이 같은 급으로 보입니다.
 *
 * 스타일 본문은 `assets/map_style.json` 입니다 — 라벨 레이어 22개는 CARTO 공식
 * Positron GL 스타일에서 발췌한 것이라 코드 문자열에 담기엔 큽니다.
 * 여기서는 읽어서 프록시 포트만 끼웁니다.
 *
 * CARTO 무료 타일은 **출처 표기가 조건**입니다 — attribution 을 지우지 마세요.
 */
internal object OsmStyle {

    /**
     * 사진 채움을 끼울 기준 — 이 레이어 **아래**가 "바탕 위, 라벨 아래" 입니다.
     * `map_style.json` 의 투명 앵커 레이어와 이름이 같아야 합니다.
     */
    const val LABELS_LAYER = "labels-anchor"

    /**
     * [proxyPort] 는 바탕 타일을 픽셀화해 주는 [TileProxy] 의 문.
     * [dark] 면 밤 지도 — 배경·라벨 색만 뒤집습니다(타일은 프록시가 갈아 끼웁니다).
     * 스타일 파일의 색 토큰(#EAE8E4·#1B1B1B·#FFFFFF)은 배경·라벨에만 쓰여
     * 문자열 치환이 안전합니다 — 새 레이어를 넣을 때 이 색을 다른 데 쓰지 마세요.
     */
    fun json(context: Context, proxyPort: Int, dark: Boolean): String {
        var style = context.assets.open("map_style.json").bufferedReader().use { it.readText() }
            .replace("{PROXY_PORT}", proxyPort.toString())
        if (dark) {
            style = style
                .replace("#EAE8E4", "#2A2A2A")   // 바탕이 오기 전의 배경
                .replace("#1B1B1B", "#E8E6E1")   // 라벨 글자 — 어두운 지도 위의 밝은 잉크
                .replace("#FFFFFF", "#20201E")   // 라벨 테두리
        }
        return style
    }
}
