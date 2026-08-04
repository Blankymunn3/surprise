package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/**
 * 지도 | 달력 탭. 밑줄이 아니라 **알약 세그먼트**입니다 —
 * 지도 위에 떠 있어야 해서 배경이 필요하고, 밑줄은 지도 위에서 보이지 않습니다.
 *
 * [floating] 이 true 면 유리(지도 위), false 면 회색 바탕(종이 위)입니다.
 * 같은 부품인데 놓이는 층이 다를 뿐입니다.
 */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEachIndexed { index, label ->
                val on = index == selectedIndex
                Box(
                    modifier = Modifier
                        .then(
                            if (on) Modifier
                                .shadow(2.dp, MemoryShapes.Pill)
                                .clip(MemoryShapes.Pill)
                                .background(MemoryColors.Surface)
                            else Modifier.clip(MemoryShapes.Pill)
                        )
                        .clickable { onSelect(index) }
                        .semantics { this.selected = on; this.role = Role.Tab }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MemoryType.Button,
                        color = if (on) MemoryColors.Ink else MemoryColors.Ink2,
                    )
                }
            }
        }
    }

    if (floating) {
        FloatingSurface(modifier = modifier) { content() }
    } else {
        Box(
            modifier = modifier
                .clip(MemoryShapes.Pill)
                .background(MemoryColors.Fill)
        ) { content() }
    }
}
