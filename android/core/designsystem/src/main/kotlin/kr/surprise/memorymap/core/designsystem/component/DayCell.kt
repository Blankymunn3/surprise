package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/** 숫자 위에 까는 그늘의 높이. 숫자 한 줄만 덮으면 됩니다. */
private val ShadeHeight = 22.dp

/**
 * 달력의 하루 칸.
 *
 * 숫자는 **늘 왼쪽 위**입니다 — 자리가 날마다 바뀌면 눈이 숫자를 다시 찾아야 합니다.
 *
 * 빈 날에는 **아무것도 깔지 않습니다.** 칸마다 회색을 깔면 달력이 격자무늬가 되어,
 * 정작 봐야 할 사진이 묻힙니다. 배경은 사진이 있을 때만 생기고, 그래서 사진이 있는
 * 날이 저절로 눈에 띕니다.
 *
 * 테두리는 두 가지입니다 — **고른 날은 잉크, 오늘은 레드.** 둘 다면 고른 쪽이
 * 이깁니다. 지금 무엇을 보고 있는지가, 오늘이 언제인지보다 급합니다.
 */
@Composable
fun DayCell(
    day: Int,
    photoUrl: String?,
    isToday: Boolean,
    isSunday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val hasPhoto = photoUrl != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // 밝은 사진 위에서도 흰 숫자가 읽히도록 **위쪽 한 줄만** 어둡게.
            // 칸 전체를 덮으면 사진이 어두워지고, 사진을 보려고 만든 칸이 아니게 됩니다.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ShadeHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(MemoryColors.Ink.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
            )
        }

        Text(
            text = day.toString(),
            style = MemoryType.Micro,
            color = when {
                hasPhoto -> Color.White
                isSunday -> MemoryColors.AccentDeep
                else -> MemoryColors.Ink
            },
            modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 3.dp),
        )

        // 사진 위에 그려야 하므로 칸 배경이 아니라 맨 위 층입니다
        val edge = when {
            isSelected -> MemoryColors.Ink
            isToday -> MemoryColors.Accent
            else -> null
        }
        if (edge != null) {
            Box(Modifier.matchParentSize().border(2.dp, edge))
        }
    }
}
