package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/**
 * 달력의 하루 칸.
 *
 * 숫자는 **늘 왼쪽 위**입니다 — 자리가 날마다 바뀌면 눈이 숫자를 다시 찾아야 합니다.
 *
 * 빈 날에는 **아무것도 깔지 않습니다.** 칸마다 회색을 깔면 달력이 격자무늬가 되어,
 * 정작 봐야 할 사진이 묻힙니다. 배경은 사진이 있을 때만 생기고, 그래서 사진이 있는
 * 날이 저절로 눈에 띕니다.
 */
@Composable
fun DayCell(
    day: Int,
    photoUrl: String?,
    isToday: Boolean,
    isSunday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPhoto = photoUrl != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MemoryShapes.DayCell)
            .clickable(onClick = onClick)
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // 밝은 파스텔 사진 위에서도 흰 숫자가 읽히도록 왼쪽 위만 어둡게
            Box(
                Modifier.matchParentSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x9E140C10), Color.Transparent),
                        center = Offset.Zero,
                        radius = 220f,
                    )
                )
            )
        }

        Text(
            text = day.toString(),
            style = MemoryType.Label,
            color = when {
                hasPhoto -> Color.White
                isSunday -> MemoryColors.Accent
                else -> MemoryColors.Ink2
            },
            modifier = Modifier.align(Alignment.TopStart).padding(start = 7.dp, top = 5.dp),
        )

        if (isToday) {
            // 사진 위에 그려야 하므로 칸 배경이 아니라 맨 위 층입니다
            Box(Modifier.matchParentSize().border(2.dp, MemoryColors.Accent, MemoryShapes.DayCell))
        }
    }
}
