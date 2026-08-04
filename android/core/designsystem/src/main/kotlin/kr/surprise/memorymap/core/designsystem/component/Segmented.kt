package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/** 탭 높이. 시안이 정한 값입니다. */
private val TabHeight = 40.dp

/**
 * 지도 | 달력 탭. **가로를 꽉 채운 네모 두 칸**입니다.
 *
 * 알약이 아닌 이유: 이 탭은 지도 위에 떠 있지 않고 지도 **위쪽에 자리를 차지하고**
 * 있습니다. 떠 있지 않으니 알약으로 만들어 배경과 떼어 놓을 까닭이 없고,
 * 가로를 꽉 채우면 두 칸이 정확히 반씩이라 어느 쪽이 켜졌는지 한눈에 보입니다.
 *
 * 고른 칸은 **잉크로 꽉 채웁니다** — 선만으로는 두 칸 중 어느 쪽인지 헷갈립니다.
 */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(TabHeight)
            .border(MemoryStroke.Border, MemoryColors.Line),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selectedIndex

            // 칸 사이 선은 **한 줄만** 긋습니다. 칸마다 테두리를 두르면 가운데가
            // 두 겹이 되어 그 선만 굵어 보입니다.
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(MemoryStroke.Border)
                        .background(MemoryColors.Line)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) MemoryColors.Ink else MemoryColors.Surface)
                    .clickable { onSelect(index) }
                    .semantics { this.selected = on; this.role = Role.Tab },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MemoryType.Body,
                    color = if (on) MemoryColors.Paper else MemoryColors.Ink,
                )
            }
        }
    }
}
