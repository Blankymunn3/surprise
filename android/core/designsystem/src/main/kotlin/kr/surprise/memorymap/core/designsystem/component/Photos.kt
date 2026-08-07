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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.R
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes

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
    // 패미컴 스타일에서는 사진이 **검정 판 위**에 놓입니다. 기준 디자인의 밝은 회색
    // 자리(Fill)를 그대로 두면 사진이 없는 칸만 하얗게 떠서, 정작 사진보다 눈에 띕니다.
    // 대표사진 표시는 두 스타일 다 레드라 그대로 갑니다 — 이 앱에서 레드는 한 벌입니다.
    val shape = PlasticShapes.Chip
    val empty = PlasticColors.PlateLo
    val cover = PlasticColors.Red

    Box(
        modifier = modifier
            .clip(shape)
            .background(empty)
            .then(if (isCover) Modifier.border(2.dp, cover, shape) else Modifier)
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
                    .background(cover),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MemoryIcons.Star, contentDescription = stringResource(R.string.component_cover_photo), tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}
