package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/**
 * 사진 썸네일. 비율 1:1 고정 — 세로 사진도 가운데를 잘라 씁니다.
 * 대표사진은 로즈 2px 테두리 + ★.
 */
@Composable
fun PhotoThumb(
    url: String?,
    modifier: Modifier = Modifier,
    isCover: Boolean = false,
    dateLabel: String? = null,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(MemoryShapes.Thumb)
            .background(MemoryColors.Fill)
            .then(if (isCover) Modifier.border(2.dp, MemoryColors.Accent, MemoryShapes.Thumb) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (dateLabel != null) {
            // 밝은 사진 위에서도 읽히도록 아래쪽에만 옅은 그늘을 깝니다
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.72f to Color.Transparent,
                            1f to Color(0x6B140C10),
                        )
                    )
            )
            Text(
                text = dateLabel,
                style = MemoryType.Micro,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 6.dp),
            )
        }

        if (isCover) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(MemoryShapes.Pill)
                    .background(MemoryColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MemoryIcons.Star, contentDescription = "대표사진", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}
