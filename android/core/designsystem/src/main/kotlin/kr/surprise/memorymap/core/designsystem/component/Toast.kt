package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes

/**
 * 앱이 사용자에게 한마디 할 때 쓰는 자리.
 *
 * `SnackbarHost` 를 그대로 쓰면 **머티리얼 기본 모양**(둥근 진회색 알약, 로보토)이
 * 나옵니다. 이 앱은 두 스타일 다 그것과 닮은 데가 없어서, 메시지가 뜰 때마다
 * 남의 앱 조각이 끼어든 것처럼 보입니다. 줄 세우기·시간 재기·큐 관리는 머티리얼이
 * 잘하므로 **그릇만 우리 것으로 갈아 끼웁니다.**
 *
 * 버튼(action)은 두지 않습니다. 이 앱의 메시지는 전부 "알려 주기" 라서
 * 되돌리기·다시 시도 같은 것이 붙지 않습니다 — 다시 시도는 그 화면 안에 있습니다.
 */
@Composable
fun MemoryToast(host: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(host, modifier) { data ->
        // 검정 판에 얹은 글자. 기기가 말하는 것이라 화면 색으로 그립니다.
        Text(
            text = data.visuals.message,
            style = MemoryType.Label,
            color = PlasticColors.OnPlate,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(PlasticShapes.Chip)
                .background(PlasticColors.Plate)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}
