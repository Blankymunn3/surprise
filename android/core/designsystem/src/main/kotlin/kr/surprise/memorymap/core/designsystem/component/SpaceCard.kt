package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.R
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/** 카드 높이는 고정입니다. 사진 비율이 제각각이어도 목록의 리듬은 일정해야 합니다. */
private val CardHeight = 150.dp

/**
 * 공간 목록 카드. **사진이 카드 전체**입니다 —
 * 공간을 알아보는 건 이름이 아니라 사진이라서요.
 *
 * 사진이 없으면 그늘을 깔지 않고 글자를 잉크로 씁니다. 회색 면에 흰 글자를
 * 얹으려고 그늘을 넣으면, 사진도 없는 카드가 괜히 어두워집니다.
 */
@Composable
fun SpaceCard(
    name: String,
    meta: String,
    coverUrl: String?,
    memberInitials: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 혼자 쓰는 짜국이면 왼쪽 위에 '이 폰에만' 이 붙습니다. */
    onlyOnThisPhone: Boolean = false,
) {
    val hasCover = coverUrl != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .shadow(6.dp, MemoryShapes.Card, spotColor = MemoryColors.Ink)
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

            // 흰 글자가 밝은 사진 위에서도 읽히도록 아래쪽만 어둡게.
            // 사진에 색을 입히는 틴트가 아니라, 글자 있는 쪽에만 두는 그늘입니다.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.38f to Color.Transparent,
                            1f to MemoryColors.Ink.copy(alpha = 0.78f),
                        )
                    )
            )
        }

        if (onlyOnThisPhone) {
            OnlyOnThisPhone(Modifier.align(Alignment.TopStart).padding(12.dp))
        }

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MemoryType.Title,
                    color = if (hasCover) Color.White else MemoryColors.Ink,
                )
                Text(
                    text = meta,
                    style = MemoryType.Micro,
                    color = if (hasCover) Color.White.copy(alpha = 0.82f) else MemoryColors.Ink2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            MemberAvatars(initials = memberInitials, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

/**
 * 사진이 이 기기 안에만 있다는 표시.
 *
 * **다는 쪽이 예외입니다** — 같이 쓰는 짜국에는 아무것도 달지 않습니다. 둘 다 달면
 * 목록이 딱지투성이가 되고 어느 쪽이 특별한지도 알 수 없습니다.
 *
 * 레드를 쓰지 않는 이유: 누르는 것이 아니라 그냥 알려 주는 것이라서요.
 * 레드는 주 동작·대표·오늘·에러에만 씁니다.
 */
@Composable
private fun OnlyOnThisPhone(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.component_only_on_this_phone),
        style = MemoryType.Micro,
        color = MemoryColors.Ink,
        modifier = modifier
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 카드 위 멤버 칩은 셋까지, 넘으면 잉크 칸에 +N. */
private const val AVATARS_SHOWN = 3

/**
 * 이름 첫 글자 칩. **네모에 흰 면, 1px 잉크 선입니다.**
 *
 * 사람마다 색을 주지 않는 이유: 카드 대부분이 사진이라 여기에 색을 더하면
 * 사진과 색이 부딪힙니다. 사람을 구분하는 건 색이 아니라 글자입니다.
 */
@Composable
fun MemberAvatars(
    initials: List<String>,
    modifier: Modifier = Modifier,
    max: Int = AVATARS_SHOWN,
) {
    val shown = initials.take(max)
    val rest = initials.size - shown.size

    Row(modifier) {
        shown.forEachIndexed { index, text ->
            AvatarChip(text = text, index = index)
        }
        if (rest > 0) {
            AvatarChip(text = "+$rest", index = shown.size, filled = true)
        }
    }
}

@Composable
private fun AvatarChip(text: String, index: Int, filled: Boolean = false) {
    Box(
        Modifier
            .offset(x = (-6 * index).dp)
            .size(24.dp)
            .background(if (filled) MemoryColors.Ink else MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MemoryType.Micro,
            color = if (filled) MemoryColors.OnAccent else MemoryColors.Ink,
        )
    }
}
