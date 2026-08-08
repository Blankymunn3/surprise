package kr.jjaguk.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 한 번 누를 때 옮기는 배율. 1단계면 넓이가 절반이 됩니다 — 두 손가락으로 벌리는 것과
 * 비슷한 폭입니다. 패미컴 스타일 지도(`MapPlastic.kt`)도 같은 값을 씁니다 —
 * 코틀린에서 최상위 `private` 은 파일 안까지라 `internal` 이어야 보입니다.
 */
internal const val ZOOM_STEP = 1.0

/**
 * 지도 탭. 지도가 이 칸을 **꽉 채우고**, 조작하는 것만 그 위에 떠 있습니다.
 *
 * 지도 위 상주물은 **검색칸과 ＋ 둘뿐입니다.** 나머지(줌·내 위치)는 조작 중에만
 * 쓰는 것이라 왼쪽 아래로 몰아 두고, 지역 시트는 눌렀을 때만 올라옵니다.
 *
 * 시트가 올라오면 ＋ 와 지도 버튼도 **같이 올라갑니다** — 가려진 채로 남으면 못 누릅니다.
 */
@Composable
fun MapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 조작하는 것이 지도 위가 아니라 몸통 위(화면 밖)에 섭니다.
    Box(modifier.fillMaxSize()) { PlasticMapBody(state, onIntent) }
}
