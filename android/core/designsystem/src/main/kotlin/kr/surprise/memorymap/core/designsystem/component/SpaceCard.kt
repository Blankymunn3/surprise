package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/**
 * 공간 목록 카드. **사진이 카드 전체**입니다 —
 * 공간을 알아보는 건 이름이 아니라 사진이라서요.
 */
@Composable
fun SpaceCard(
    name: String,
    meta: String,
    coverUrl: String?,
    memberInitials: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9.6f)
            .shadow(10.dp, MemoryShapes.Card, spotColor = MemoryColors.Ink)
            .clip(MemoryShapes.Card)
            .background(MemoryColors.Fill)
            .clickable(onClick = onClick)
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        // 흰 글자가 밝은 사진 위에서도 읽히도록 아래쪽을 어둡게
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.34f to Color.Transparent,
                        0.56f to Color(0x24140C10),
                        1f to Color(0x9E140C10),
                    )
                )
        )

        MemberAvatars(
            initials = memberInitials,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        )

        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 15.dp)) {
            Text(name, style = MemoryType.Title, color = Color.White)
            Text(meta, style = MemoryType.Label, color = Color.White.copy(alpha = 0.88f))
        }
    }
}

/** 이름 첫 글자 원. 넷을 넘으면 마지막이 +N 이 됩니다. */
@Composable
fun MemberAvatars(
    initials: List<String>,
    modifier: Modifier = Modifier,
    max: Int = 4,
) {
    val shown = if (initials.size <= max) initials else initials.take(max - 1) + "+${initials.size - (max - 1)}"

    Row(modifier) {
        shown.forEachIndexed { index, text ->
            Box(
                Modifier
                    .offset(x = (-7 * index).dp)
                    .size(26.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.92f), MemoryShapes.Pill)
                    .clip(MemoryShapes.Pill)
                    .background(avatarColor(text)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text, style = MemoryType.Micro, color = Color.White)
            }
        }
    }
}

/**
 * 이름에서 색을 정합니다. 같은 사람은 늘 같은 색이어야 해서 난수를 쓰지 않습니다.
 * 강조색(로즈)은 첫 번째 자리에만 두고, 나머지는 사진과 부딪히지 않는 차분한 색으로.
 */
private val AvatarPalette = listOf(
    MemoryColors.Accent,
    Color(0xFF9B8FC8),
    Color(0xFF82B7B2),
    Color(0xFFCDAA77),
    Color(0xFF7FA6C4),
    Color(0xFF88BE9C),
)

private fun avatarColor(text: String): Color =
    AvatarPalette[(text.sumOf { it.code }.mod(AvatarPalette.size))]
