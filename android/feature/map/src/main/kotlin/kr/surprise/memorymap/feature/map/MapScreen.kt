package kr.surprise.memorymap.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

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

/**
 * 지도 위에 떠 있는 것들이 터치를 **먹게** 합니다.
 *
 * 지도는 AndroidView(MapLibre) 라 자기 방식으로 터치를 받습니다. 그 위에 얹은 Compose
 * 요소가 터치를 소비하지 않으면 **밑의 지도까지 같이 눌립니다** — 시트 안의 버튼을 눌렀는데
 * 시트 뒤쪽 지역이 함께 선택되던 것이 이것 때문입니다.
 *
 * **Main 단계**에서 먹습니다. 이 단계는 자식부터 위로 올라오므로, 글자칸·버튼이 **먼저**
 * 받고 남은 것만 우리가 먹습니다. Initial 에서 먹으면 자식한테 가기도 전에 가로채서
 * 검색창을 눌러도 자판이 안 올라오고 시트 버튼도 안 눌립니다.
 */
private fun Modifier.blockMapTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Main).changes.forEach {
                if (!it.isConsumed) it.consume()
            }
        }
    }
}
